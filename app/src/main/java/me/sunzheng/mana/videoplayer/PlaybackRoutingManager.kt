package me.sunzheng.mana.videoplayer

import android.content.Context
import android.util.Log
import me.sunzheng.mana.R
import me.sunzheng.mana.core.net.v2.routing.BackendDto
import me.sunzheng.mana.core.net.v2.routing.ROUTING_LOG_TAG
import me.sunzheng.mana.core.net.v2.routing.RouteHttpOutcome
import me.sunzheng.mana.core.net.v2.routing.RoutePreference
import me.sunzheng.mana.core.net.v2.routing.RouteResult
import me.sunzheng.mana.core.net.v2.routing.RoutingDataSource
import me.sunzheng.mana.core.net.v2.routing.RoutingPreferences
import me.sunzheng.mana.core.net.v2.routing.RoutingPreference
import me.sunzheng.mana.core.net.v2.routing.RoutingResolver

/**
 * 一次播放会话内的路由编排（FR-1/3/4/5/6）。
 *
 * 会话 = 一个 `VideoPlayerActivity` 实例（含切集/切源）；排除集、故障转移预算都只留内存（§6.2/§6.3），
 * 切集或切源（canonical 变化）时重置预算。
 */
class PlaybackRoutingManager(
    private val context: Context,
    private val dataSource: RoutingDataSource,
    private val preferences: RoutingPreferences,
    private val host: String,
    private val viewModel: VideoPlayerVideoModel
) {

    /** 本会话判定失败的 backendId（§6.2：只留内存、不落盘）。 */
    private val excludedBackendIds = LinkedHashSet<String>()

    /** 本会话故障转移时间戳（同一 canonical 每 10 分钟 ≤ 2 次、间隔 ≥ 30s）。 */
    private val failoverTimestamps = ArrayDeque<Long>()

    private var sessionCanonical: String? = null
    private var currentBackend: BackendDto? = null

    /** 预算用尽 / 路由不可用后不再自动重试（FR-6）。 */
    private var failoverExhausted = false

    fun currentBackend(): BackendDto? = currentBackend

    /** 本会话已执行的故障转移次数（用于 L17 日志）。 */
    fun failoverCount(): Int = failoverTimestamps.size

    /** canonical URL（HostUtil.makeUp 的等价实现）；host 空白或 url 为空 → null（§11：调用前 guard）。 */
    fun canonicalUrlOf(videoFileUrl: String?): String? =
        RoutingResolver.canonicalUrlOf(host, videoFileUrl)

    /** 会话开始 / 切换资源：canonical 变化时清空排除集与预算（§6.3）。 */
    fun startSession(canonicalUrl: String) {
        if (canonicalUrl != sessionCanonical) {
            sessionCanonical = canonicalUrl
            excludedBackendIds.clear()
            failoverTimestamps.clear()
            failoverExhausted = false
            currentBackend = null
            viewModel.currentNodeLiveData.postValue(null)
        }
        Log.d(ROUTING_LOG_TAG, "session start host=$host canonical=$canonicalUrl") // L1
    }

    // ------------------------------------------------------------------ FR-1 能力探测

    /** 能力探测（FR-1）：结果写进 SP；返回是否可用。绝不阻塞播放（调用方自己并发）。 */
    suspend fun probeCapability(canonicalUrl: String): Boolean {
        val origin = RoutingResolver.originOf(canonicalUrl)
        if (origin == null) {
            Log.d(ROUTING_LOG_TAG, "skip routing reason=no-origin") // L11
            viewModel.routingSupportedLiveData.postValue(false)
            return false
        }
        if (preferences.isCapabilitySuppressed()) {
            val remaining = (RoutingPreferences.CAPABILITY_SUPPRESS_WINDOW_MS -
                (System.currentTimeMillis() - preferences.capabilityCheckedAt())).coerceAtLeast(0L)
            Log.d(ROUTING_LOG_TAG, "probe skipped reason=suppressed remainingMs=$remaining") // L5
            val supported = preferences.capability() == RoutingPreferences.CAPABILITY_YES
            viewModel.routingSupportedLiveData.postValue(supported)
            return supported
        }

        Log.d(ROUTING_LOG_TAG, "probe origin=$origin") // L3
        val outcome = dataSource.backendsOutcome(origin)
        val supported = outcome.isSupported
        Log.d(
            ROUTING_LOG_TAG,
            "probe result code=${outcome.code} supported=$supported elapsedMs=${outcome.elapsedMs}"
        ) // L4
        if (supported) {
            dataSource.logBackends(outcome) // L6
            preferences.setCapability(RoutingPreferences.CAPABILITY_YES)
            preferences.cacheBackends(outcome.backends ?: emptyList())
        } else {
            preferences.setCapability(RoutingPreferences.CAPABILITY_NO)
        }
        viewModel.routingSupportedLiveData.postValue(supported)
        publishNodes(cached = false)
        return supported
    }

    // ------------------------------------------------------------------ FR-2 节点列表

    /** 面板刷新（FR-2）：重新拉取一次列表；失败返回 null 由调用方展示缓存/提示。 */
    suspend fun refreshBackends(canonicalUrl: String): List<BackendDto>? {
        val origin = RoutingResolver.originOf(canonicalUrl) ?: return null
        val list = dataSource.listBackends(origin) // L6
        if (list != null) {
            preferences.cacheBackends(list)
            publishNodes(cached = false)
        }
        return list
    }

    /** 用缓存的列表渲染面板（先渲染缓存，FR-2）。 */
    fun publishCachedNodes() = publishNodes(cached = true)

    private fun publishNodes(cached: Boolean) {
        val backends = preferences.cachedBackends() ?: emptyList()
        viewModel.nodeListLiveData.postValue(
            NodeAdapter.buildItems(
                backends = backends,
                // FR-2/FR-3：标记"实际生效"的节点；还没路由过时回落展示偏好节点
                currentBackendId = currentBackend?.id ?: preferences.preference().backendId,
                cachedLabel = if (cached && backends.isNotEmpty()) {
                    context.getString(R.string.node_list_cached)
                } else {
                    null
                },
                otherGroupLabel = context.getString(R.string.node_group_other)
            )
        )
    }

    // ------------------------------------------------------------------ FR-5 路由决策

    /**
     * 是否需要请求路由（FR-5）。非 suspend：auto 路径必须与改动前完全一致（不引入协程跳转/额外延迟）。
     */
    fun needRoute(): Boolean {
        val preference = preferences.preference()
        val needRoute = preference.isBackend
        Log.d(
            ROUTING_LOG_TAG,
            "route decision needRoute=$needRoute reason=" +
                (if (needRoute) "preference-backend" else "auto-no-override") +
                " mode=${preference.mode}"
        ) // L7
        if (!needRoute) {
            currentBackend = null
            viewModel.currentNodeLiveData.postValue(null)
        }
        return needRoute
    }

    /**
     * 起播时是否需要请求路由（FR-5）。返回 null = 直接用 canonical（不请求）。
     */
    suspend fun resolveIfNeeded(canonicalUrl: String): RouteResult? {
        if (!needRoute()) return null
        val preference = preferences.preference()
        return resolve(
            canonicalUrl = canonicalUrl,
            preference = RoutePreference(RoutingPreferences.MODE_BACKEND, preference.backendId),
            exclude = emptyList(),
            allowPreferenceReset = true
        )
    }

    /** FR-4：用户手动选定后立即解析（auto → 让服务端选；backend → 指定节点）。 */
    suspend fun resolveForManual(
        canonicalUrl: String,
        preference: RoutingPreference
    ): RouteResult = resolve(
        canonicalUrl = canonicalUrl,
        preference = if (preference.isBackend) {
            RoutePreference(RoutingPreferences.MODE_BACKEND, preference.backendId)
        } else {
            RoutePreference(RoutingPreferences.MODE_AUTO)
        },
        exclude = emptyList(),
        allowPreferenceReset = true
    )

    // ------------------------------------------------------------------ FR-6 故障转移

    fun canFailover(now: Long = System.currentTimeMillis()): Boolean {
        pruneFailoverTimestamps(now)
        if (failoverExhausted) return false
        if (failoverTimestamps.size >= MAX_FAILOVERS_PER_WINDOW) return false
        val last = failoverTimestamps.lastOrNull() ?: return true
        return now - last >= MIN_FAILOVER_INTERVAL_MS
    }

    /**
     * 自动故障转移（FR-6）：加排除项 + `preference.mode="auto"`。
     * 预算用尽/不可用时返回 [RouteResult.Fallback] 且 reason = [REASON_BUDGET_EXHAUSTED]。
     * **不修改**已持久化的用户偏好。
     */
    suspend fun failover(canonicalUrl: String, failedBackendId: String?): RouteResult {
        val now = System.currentTimeMillis()
        pruneFailoverTimestamps(now)
        val withinInterval = failoverTimestamps.lastOrNull()?.let { now - it < MIN_FAILOVER_INTERVAL_MS } ?: false
        if (failoverExhausted || failoverTimestamps.size >= MAX_FAILOVERS_PER_WINDOW || withinInterval) {
            failoverExhausted = true
            Log.d(ROUTING_LOG_TAG, "failover exhausted count=${failoverTimestamps.size} action=canonical") // L17
            return RouteResult.Fallback(canonicalUrl, REASON_BUDGET_EXHAUSTED)
        }

        if (!failedBackendId.isNullOrBlank()) {
            excludedBackendIds.add(failedBackendId)
            while (excludedBackendIds.size > MAX_EXCLUDE_BACKENDS) {
                val oldest = excludedBackendIds.firstOrNull() ?: break
                excludedBackendIds.remove(oldest)
            }
        }
        Log.d(
            ROUTING_LOG_TAG,
            "failover attempt=${failoverTimestamps.size + 1} exclude=${excludedBackendIds.size} " +
                "from=${failedBackendId ?: "-"}"
        ) // L16
        val result = resolve(
            canonicalUrl = canonicalUrl,
            preference = RoutePreference(RoutingPreferences.MODE_AUTO),
            exclude = excludedBackendIds.toList(),
            allowPreferenceReset = false
        )
        val ok = result is RouteResult.Routed
        Log.d(
            ROUTING_LOG_TAG,
            "failover result ok=$ok newBackend=${(result as? RouteResult.Routed)?.backend?.id ?: "-"}"
        ) // L16
        if (ok) {
            failoverTimestamps.addLast(now)
        } else {
            // 路由不可用 → 停止自动重试（回落 canonical）
            failoverExhausted = true
        }
        return result
    }

    // ------------------------------------------------------------------ 内部

    private suspend fun resolve(
        canonicalUrl: String,
        preference: RoutePreference,
        exclude: List<String>,
        allowPreferenceReset: Boolean
    ): RouteResult {
        val origin = RoutingResolver.originOf(canonicalUrl)
        val resource = RoutingResolver.resourceOf(canonicalUrl)
        if (origin == null || resource == null) {
            Log.d(ROUTING_LOG_TAG, "fallback reason=bad-canonical code=-1") // L11
            return RouteResult.Fallback(canonicalUrl, REASON_BAD_CANONICAL)
        }

        Log.d(
            ROUTING_LOG_TAG,
            "route request mode=${preference.mode} backendId=${preference.backendId ?: "-"} " +
                "exclude=${exclude.size} resource=$resource"
        ) // L8
        val outcome = dataSource.createRoute(origin, resource, preference, exclude)
        Log.d(
            ROUTING_LOG_TAG,
            "route response code=${outcome.code} selected=${outcome.response?.selectedBackend?.id ?: "-"} " +
                "selectionMode=${outcome.response?.selection?.mode ?: "-"} " +
                "reason=${outcome.response?.selection?.reason ?: "-"} elapsedMs=${outcome.elapsedMs}"
        ) // L9

        // §4.2：422 = 偏好非法或节点不可选 → 重置为自动、刷新列表、最多重试 1 次
        if (outcome.code == RoutingDataSource.CODE_INVALID_PREFERENCE &&
            preference.mode == RoutingPreferences.MODE_BACKEND &&
            allowPreferenceReset
        ) {
            Log.d(ROUTING_LOG_TAG, "fallback reason=preference-rejected code=${outcome.code}") // L11
            preferences.setAuto()
            publishNodes(cached = false)
            return resolve(
                canonicalUrl = canonicalUrl,
                preference = RoutePreference(RoutingPreferences.MODE_AUTO),
                exclude = exclude,
                allowPreferenceReset = false
            )
        }

        if (!outcome.isCreated) {
            Log.d(ROUTING_LOG_TAG, "fallback reason=${fallbackReason(outcome)} code=${outcome.code}") // L11
            return RouteResult.Fallback(canonicalUrl, fallbackReason(outcome))
        }

        val body = outcome.response!!
        val token = body.routeToken!!
        val playbackUrl = body.playbackUrl!!
        val valid = RoutingResolver.isValidPlaybackUrl(canonicalUrl, token, playbackUrl)
        Log.d(ROUTING_LOG_TAG, "route url check ok=$valid") // L10（不打印 URL）
        if (!valid) {
            Log.d(ROUTING_LOG_TAG, "fallback reason=url-check code=${outcome.code}") // L11
            return RouteResult.Fallback(canonicalUrl, REASON_URL_CHECK)
        }

        currentBackend = body.selectedBackend
        viewModel.currentNodeLiveData.postValue(currentBackend)
        return RouteResult.Routed(canonicalUrl, playbackUrl, body.selectedBackend)
    }

    /** §4.2 的错误码 → 降级原因（不带 token）。 */
    private fun fallbackReason(outcome: RouteHttpOutcome): String = when (outcome.code) {
        RoutingDataSource.CODE_FAILURE -> "network"
        400 -> "bad-request"
        403 -> "origin-not-allowed"
        404, 405 -> "unsupported"
        429 -> "rate-limited"
        503 -> "no-healthy-backend"
        else -> "http-${outcome.code}"
    }

    private fun pruneFailoverTimestamps(now: Long) {
        while (true) {
            val first = failoverTimestamps.firstOrNull() ?: return
            if (now - first > FAILOVER_WINDOW_MS) failoverTimestamps.removeFirst() else return
        }
    }

    companion object {
        const val REASON_BUDGET_EXHAUSTED = "budget-exhausted"
        const val REASON_URL_CHECK = "url-check"
        const val REASON_BAD_CANONICAL = "bad-canonical"

        /** 同一资源每 10 分钟自动故障转移 ≤ 2 次。 */
        const val MAX_FAILOVERS_PER_WINDOW = 2
        const val FAILOVER_WINDOW_MS = 10L * 60 * 1000

        /** 两次故障转移最小间隔 ≥ 30s。 */
        const val MIN_FAILOVER_INTERVAL_MS = 30L * 1000

        /** 排除集上限 16（§4.1）。 */
        const val MAX_EXCLUDE_BACKENDS = 16
    }
}

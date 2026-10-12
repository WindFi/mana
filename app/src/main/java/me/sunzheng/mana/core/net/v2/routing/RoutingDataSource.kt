package me.sunzheng.mana.core.net.v2.routing

import android.util.Log
import kotlinx.coroutines.CancellationException
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** GET backends 的原始结果（code 为 -1 表示网络失败/超时/解析失败）。 */
data class BackendsOutcome(
    val code: Int,
    val backends: List<BackendDto>?,
    val elapsedMs: Long
) {
    val isSupported: Boolean get() = code == 200 && backends != null
}

/** POST routes 的原始结果（code 为 -1 表示网络失败/超时/解析失败）。 */
data class RouteHttpOutcome(
    val code: Int,
    val response: RouteResponse?,
    val elapsedMs: Long
) {
    val isCreated: Boolean get() = code == 201 && response?.playbackUrl != null && response.routeToken != null
}

/**
 * routing API 的数据源（§8.1）：
 * - 按 origin 缓存 Retrofit 实例（origin 只能从 canonical URL 推导，见 §4.3 红线 9）；
 * - 专用 OkHttpClient：**完全不带 cookie**（对应 mira 的 credentials:'omit'）、短超时、日志只记 BASIC（**禁止 BODY**，§FR-11）。
 *
 * 说明：不能从 `defaultOkHttpClient` 派生 —— 它挂了 BODY 级别的 network interceptor，`newBuilder()` 无法移除，
 * 只能新建一个干净 client，以满足"routing 请求禁止 BODY 日志"的红线。
 */
@Singleton
class RoutingDataSource @Inject constructor() {

    private val apis = ConcurrentHashMap<String, RoutingApi>()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(CookieJar.NO_COOKIES)
            .callTimeout(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .connectTimeout(CALL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            .build()
    }

    private fun api(origin: String): RoutingApi = apis.getOrPut(origin) {
        Retrofit.Builder()
            .baseUrl("$origin/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RoutingApi::class.java)
    }

    /** 原始结果，不打日志（探测路径自己按 L4/L6 打点）。 */
    suspend fun backendsOutcome(origin: String): BackendsOutcome {
        val startedAt = System.currentTimeMillis()
        return try {
            val response = api(origin).backends()
            val body = response.body()
            val list = if (response.isSuccessful) body?.backends else null
            BackendsOutcome(response.code(), list, System.currentTimeMillis() - startedAt)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            // 只记异常类型：避免把可能含完整请求地址的异常消息/堆栈写进日志（§4.3 红线 5）
            Log.w(ROUTING_LOG_TAG, "backends request failed type=${e.javaClass.simpleName}")
            BackendsOutcome(CODE_FAILURE, null, System.currentTimeMillis() - startedAt)
        }
    }

    /**
     * 列出节点（FR-2）。返回 null 表示不可用/不支持（调用方一律降级）。
     * 日志 L6。
     */
    suspend fun listBackends(origin: String): List<BackendDto>? {
        val outcome = backendsOutcome(origin)
        logBackends(outcome)
        return outcome.backends
    }

    /** L6：节点列表结果（regions 只打地区码，不含任何敏感信息）。 */
    fun logBackends(outcome: BackendsOutcome) {
        val regions = outcome.backends?.mapNotNull { it.region }?.distinct()?.sorted()?.joinToString(",") ?: ""
        Log.d(
            ROUTING_LOG_TAG,
            "backends code=${outcome.code} count=${outcome.backends?.size ?: 0} regions=$regions elapsedMs=${outcome.elapsedMs}"
        )
    }

    /** 创建路由（FR-5）。返回原始状态码以便按 §4.2 分支（422 需重置偏好并重试一次）。 */
    suspend fun createRoute(
        origin: String,
        resource: String,
        preference: RoutePreference,
        exclude: List<String>
    ): RouteHttpOutcome {
        val startedAt = System.currentTimeMillis()
        return try {
            val response = api(origin).createRoute(
                RouteRequest(
                    resource = resource,
                    preference = preference,
                    excludeBackendIds = exclude.take(MAX_EXCLUDE_BACKENDS)
                )
            )
            RouteHttpOutcome(
                response.code(),
                response.body(),
                System.currentTimeMillis() - startedAt
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            // 只记异常类型：避免把可能含完整请求地址的异常消息/堆栈写进日志（§4.3 红线 5）
            Log.w(ROUTING_LOG_TAG, "routes request failed type=${e.javaClass.simpleName}")
            RouteHttpOutcome(CODE_FAILURE, null, System.currentTimeMillis() - startedAt)
        }
    }

    companion object {
        /** 请求超时 2.5s（FR-1）；起播最多只等这么久，超时即回落 canonical。 */
        const val CALL_TIMEOUT_MS = 2500L

        /** 失败哨兵码（网络错误 / 超时 / 解析失败）。 */
        const val CODE_FAILURE = -1

        /** §4.1：excludeBackendIds 最多 16 个。 */
        const val MAX_EXCLUDE_BACKENDS = 16

        /** §4.2 用到的状态码。 */
        const val CODE_OK = 200
        const val CODE_CREATED = 201
        const val CODE_INVALID_PREFERENCE = 422
    }
}

package me.sunzheng.mana.core.net.v2.routing

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import me.sunzheng.mana.utils.PreferenceManager
import javax.inject.Inject
import javax.inject.Singleton

/** 用户偏好（§2 术语表）：跨会话、跨视频生效的全局默认节点设置。 */
data class RoutingPreference(
    val mode: String,
    val backendId: String?,
    val label: String?
) {
    val isBackend: Boolean get() = mode == RoutingPreferences.MODE_BACKEND
}

/**
 * 节点偏好的持久化（需求 §6.1）：只写 `global` SP。
 *
 * **严禁**在此落盘 token / 签名播放地址 / 当前生效节点 / 排除集（§6.2）。
 */
@Singleton
class RoutingPreferences @Inject constructor(
    @ApplicationContext context: Context
) {

    private val sharedPreferences = context.getSharedPreferences(
        PreferenceManager.Global.STR_SP_NAME,
        Context.MODE_PRIVATE
    )

    fun preference(): RoutingPreference {
        val mode = sharedPreferences.getString(PreferenceManager.Global.STR_KEY_ROUTING_MODE, null)
            ?.takeIf { it == MODE_BACKEND || it == MODE_AUTO } ?: MODE_AUTO
        val id = sharedPreferences.getString(PreferenceManager.Global.STR_KEY_ROUTING_BACKEND_ID, null)
        val label = sharedPreferences.getString(PreferenceManager.Global.STR_KEY_ROUTING_BACKEND_LABEL, null)
        return if (mode == MODE_BACKEND && !id.isNullOrBlank()) {
            RoutingPreference(MODE_BACKEND, id, label)
        } else {
            RoutingPreference(MODE_AUTO, null, null)
        }
    }

    /** 清除偏好（FR-3：点击「自动」）。 */
    fun setAuto() {
        sharedPreferences.edit()
            .putString(PreferenceManager.Global.STR_KEY_ROUTING_MODE, MODE_AUTO)
            .remove(PreferenceManager.Global.STR_KEY_ROUTING_BACKEND_ID)
            .remove(PreferenceManager.Global.STR_KEY_ROUTING_BACKEND_LABEL)
            .apply()
    }

    /** 写入全局默认节点（FR-3）。 */
    fun setBackend(id: String, label: String?) {
        sharedPreferences.edit()
            .putString(PreferenceManager.Global.STR_KEY_ROUTING_MODE, MODE_BACKEND)
            .putString(PreferenceManager.Global.STR_KEY_ROUTING_BACKEND_ID, id)
            .putString(PreferenceManager.Global.STR_KEY_ROUTING_BACKEND_LABEL, label)
            .apply()
    }

    /** unknown / yes / no。 */
    fun capability(): String =
        sharedPreferences.getString(PreferenceManager.Global.STR_KEY_ROUTING_CAPABILITY, null)
            ?.takeIf { it == CAPABILITY_YES || it == CAPABILITY_NO } ?: CAPABILITY_UNKNOWN

    /** 写能力结果，同时记录探测时间（FR-1 的 30 分钟抑制）。 */
    fun setCapability(value: String, checkedAt: Long = System.currentTimeMillis()) {
        sharedPreferences.edit()
            .putString(PreferenceManager.Global.STR_KEY_ROUTING_CAPABILITY, value)
            .putLong(PreferenceManager.Global.STR_KEY_ROUTING_CAPABILITY_CHECKED_AT, checkedAt)
            .apply()
    }

    fun capabilityCheckedAt(): Long =
        sharedPreferences.getLong(PreferenceManager.Global.STR_KEY_ROUTING_CAPABILITY_CHECKED_AT, 0L)

    /** 判定 no 之后是否仍处于抑制窗口内（FR-1）。 */
    fun isCapabilitySuppressed(now: Long = System.currentTimeMillis()): Boolean =
        capability() == CAPABILITY_NO && now - capabilityCheckedAt() < CAPABILITY_SUPPRESS_WINDOW_MS

    /** 节点列表缓存（FR-2：刷新失败时展示缓存）。 */
    fun cachedBackends(): List<BackendDto>? {
        val json = sharedPreferences.getString(PreferenceManager.Global.STR_KEY_ROUTING_BACKENDS_JSON, null)
            ?: return null
        return try {
            val type = object : TypeToken<List<BackendDto>>() {}.type
            Gson().fromJson<List<BackendDto>>(json, type)?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    fun cacheBackends(list: List<BackendDto>) {
        if (list.isEmpty()) return
        try {
            sharedPreferences.edit()
                .putString(PreferenceManager.Global.STR_KEY_ROUTING_BACKENDS_JSON, Gson().toJson(list))
                .apply()
        } catch (e: Exception) {
            // 缓存失败不影响主流程
        }
    }

    companion object {
        const val MODE_AUTO = "auto"
        const val MODE_BACKEND = "backend"

        const val CAPABILITY_UNKNOWN = "unknown"
        const val CAPABILITY_YES = "yes"
        const val CAPABILITY_NO = "no"

        /** FR-1：判定 no 后抑制 30 分钟再重试。 */
        const val CAPABILITY_SUPPRESS_WINDOW_MS = 30L * 60 * 1000
    }
}

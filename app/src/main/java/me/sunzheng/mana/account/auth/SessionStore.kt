package me.sunzheng.mana.account.auth

import java.net.HttpCookie
import java.net.URI
import java.net.URISyntaxException



/**
 * 会话凭据（服务端下发的 `session` cookie）的持久化后端契约。
 *
 * 设计见 `openspec/changes/accountmanager-session-store/design.md` D1/D2：只替换「凭据存在哪」，
 * cookie 的匹配、过期与滑动续期仍由 OkHttp `cookieJar`（`CookieManager` + [me.sunzheng.mana.account.auth.PersistentHttpCookieStore]）负责。
 *
 * 契约要点：
 * - 按 **origin**（归一化键 `http://<host>`，见 [SessionOrigin]）读写，天然按 host 隔离；
 * - 实现 MUST NOT 把异常抛给请求链路：读失败返回空列表，写失败返回 `false`；
 * - 读路径 MUST 与主线程兼容（账号库下用 `peekAuthToken`，不用 `blockingGetAuthToken`，见 design D3）。
 */
interface SessionStore {

    /** 读该 origin 的凭据；无数据或读取失败都返回空列表。 */
    fun read(origin: String): List<HttpCookie>

    /** 覆盖写该 origin 的凭据；返回 `true` 表示确实落盘。`cookies` 为空等价于 [delete]。 */
    fun write(origin: String, cookies: List<HttpCookie>): Boolean

    /** 删除该 origin 的凭据（含其索引）；幂等。 */
    fun delete(origin: String)

    /** 当前存有凭据的全部 origin。 */
    fun origins(): Set<String>

    /** 清空本后端的全部凭据。 */
    fun clear()

    /**
     * 该 origin 是否存在**未过期**的凭据 —— 登录态判据的唯一来源（design D8）。
     *
     * 注意：它只表示「本地有可用凭据」，不代表服务端仍认可（服务端失效只能由 401 暴露，本变更不处理）。
     */
    fun hasUsableCredential(origin: String): Boolean = read(origin).any { !it.hasExpired() }
}



/**
 * 凭据 origin 的归一化语义。
 *
 * [normalize] 与改造前 `PersistentHttpCookieStore.cookiesUri()` **逐字一致**（强制 http、丢弃端口、丢弃 path），
 * 因此账号库/SP 两个后端的键与老数据完全对齐，多 host 天然隔离。
 *
 * 已知限制（与现状一致，非本变更引入）：`https://example.com:8443/x` 与 `http://example.com` 归一成同一个键
 * （与改造前相同，见 design Non-Goals）。
 */
object SessionOrigin {

    /** 任意请求 URI 归一成 `http://<host>`；解析不出 host 时按改造前语义返回原 uri。 */
    @JvmStatic
    fun normalize(uri: URI?): URI? {
        if (uri == null) {
            return null
        }
        return try {
            URI("http", uri.host, null, null)
        } catch (e: URISyntaxException) {
            uri // probably a URI with no host
        }
    }

    /** 归一化后的字符串键；[normalize] 返回 null 时为 null。 */
    @JvmStatic
    fun key(uri: URI?): String? = normalize(uri)?.toString()

    /**
     * 把设置页里填的 host（`global.host`，形如 `https://example.com/`）换算成凭据键 `http://example.com`。
     *
     * @return 归一化键；host 为空或解析不出 host（缺 scheme、纯相对串）时返回 null ——
     *         此时没有可归属的 origin，调用方按「未登录」处理。
     */
    @JvmStatic
    fun keyOfHost(host: String?): String? {
        val raw = host?.trim().orEmpty()
        if (raw.isEmpty()) {
            return null
        }
        val parsed = try {
            URI(raw)
        } catch (e: URISyntaxException) {
            return null
        }
        val normalized = normalize(parsed) ?: return null
        return if (normalized.host.isNullOrEmpty()) null else normalized.toString()
    }
}


/**
 * 后端选择与账号库可用性的**纯判定**（design D6、§2.1）。
 *
 * 抽成纯函数是为了在无 Robolectric 的工程里也能自动覆盖选择判定表（任务 2.1/2.4）；
 * 真正的 `AccountManager` 调用留在 [me.sunzheng.mana.account.auth.AccountManagerSessionStore]，由真机验收覆盖。
 */
object SessionBackendPolicy {

    /**
     * 账号库后端的最低版本。API ≤ 22 需要 `AUTHENTICATE_ACCOUNTS` 等旧权限（本变更不新增权限），
     * 因此只在 `Build.VERSION_CODES.M`(23) 及以上启用账号库。
     */
    const val MIN_SDK_FOR_ACCOUNT_MANAGER = 23

    /**
     * 写入 `setAuthToken` 的值长度上限。
     *
     * 账号库对 authToken 没有公开的长度上限（design Risks 已标注未核实），超过该阈值时**直接降级 SP**，
     * 避免写坏账号或写入被静默截断。
     */
    const val MAX_AUTH_TOKEN_LENGTH = 4096

    /** `SDK_INT >= 23` 且账号后端可用时才用账号库，否则一律 SP。 */
    fun useAccountManager(sdkInt: Int, accountBackendAvailable: Boolean): Boolean =
        sdkInt >= MIN_SDK_FOR_ACCOUNT_MANAGER && accountBackendAvailable

    /** 凭据值能否安全写进账号库。 */
    fun canStoreAuthToken(encodedValue: String?): Boolean =
        encodedValue != null && encodedValue.isNotEmpty() && encodedValue.length <= MAX_AUTH_TOKEN_LENGTH

    /** 账号库里的 token 是否可用（空/读不到 ⇒ 降级 SP 判据）。 */
    fun hasUsableToken(token: String?): Boolean = !token.isNullOrEmpty()

    /** 账号类型后缀（design D2）：`<applicationId>.session`，保证 debug/release 互不覆盖。 */
    const val ACCOUNT_TYPE_SUFFIX = "session"

    /** 账号类型派生：`<applicationId>.session`（原 `SessionAccountType.of`，2026-10-12 归集进本对象）。 */
    fun accountTypeOf(applicationId: String): String = "$applicationId.$ACCOUNT_TYPE_SUFFIX"
}



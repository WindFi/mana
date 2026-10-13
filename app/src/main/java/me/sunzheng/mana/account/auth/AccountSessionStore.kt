package me.sunzheng.mana.account.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.util.Log
import androidx.annotation.RequiresPermission
import java.net.HttpCookie

/**
 * 系统账号库后端（API ≥ 23，design D2/D3/D6）。
 *
 * 映射关系（2026-10-12 调整：**每 App 一个账号**）：
 * - **账号**：本 App 只建一个，名字 = [accountName]（= `@string/app_name`，所以系统「设置 → 账号」那一行显示 App 名），
 *   类型 = `<applicationId>.session`（[SessionBackendPolicy]），debug/release 互不覆盖；
 * - **凭据值** = `setAuthToken(account, "session", <JSON 包>)`，包结构 `{"<origin>": [cookie, …], …}`
 *   （[JsonSessionBundleCodec]）—— 一个账号装多个 origin，凭据仍按 origin 隔离；
 * - **元数据** = `setUserData(account, "schema_version", …)`。
 *
 * 账号**按类型查找**、不按名字：名字纯粹用于显示，将来改 App 名不会"丢"账号，也不会建出第二个。
 *
 * 老版本兼容（懒迁移）：老实现的账号名恰好是 origin。读到/写到某个 origin 而包里没有它时，
 * 先看有没有同名老账号 → 解码 → 并入包 → **读回校验一致才删老账号**（与 [MigratingSessionStore] 同一约定），
 * 所以老用户不需要一次性批量迁移，也不会因为迁移中途失败而丢凭据。
 *
 * 读路径只用 `peekAuthToken`（同步 binder、**不会回调 authenticator**）；绝不使用 `blockingGetAuthToken`（design D3）。
 * 任何操作失败都**不抛给请求链路**：写失败返回 false（由 [MigratingSessionStore] 降级 SP），读失败返回空，
 * 并各留一条 `Log.w`。
 */
class AccountManagerSessionStore(
    private val accountManager: AccountManager,
    val accountType: String,
    private val accountName: String
) : SessionStore {

    companion object {
        private const val TAG = "SessionStore"

        /** authTokenType，固定为 `session`（服务端下发的就是 session cookie）。 */
        const val AUTH_TOKEN_TYPE = "session"

        /** schema 版本元数据键（design D2 的可演进位）。 */
        const val USER_DATA_SCHEMA_VERSION = "schema_version"
    }

    override fun read(origin: String): List<HttpCookie> {
        val bundle = peekBundle(primaryAccount())
        bundle?.get(origin)?.let { if (it.isNotEmpty()) return it }

        // 老版本遗留账号（名字 = origin）：读到就顺手并入单账号
        val legacy = legacyAccount(origin) ?: return emptyList()
        val cookies = peekLegacy(legacy)
        if (cookies.isEmpty()) {
            return emptyList()
        }
        if (!mergeIntoBundle(origin, cookies)) {
            Log.w(TAG, "老账号并入单账号失败，继续用老账号读取：origin=$origin")
        }
        return cookies
    }

    override fun write(origin: String, cookies: List<HttpCookie>): Boolean {
        if (cookies.isEmpty()) {
            delete(origin)
            return true
        }
        // 读不到现有包（binder 异常/坏数据）时**不写**，否则会把其它 origin 的凭据覆盖掉
        val current = peekBundle(primaryAccount()) ?: run {
            Log.w(TAG, "现有凭据包不可用，放弃写入并降级私有存储：origin=$origin")
            return false
        }
        return writeBundle(current.toMutableMap().apply { put(origin, cookies) }, origin)
    }

    override fun delete(origin: String) {
        try {
            legacyAccount(origin)?.let { accountManager.removeAccountExplicitly(it) }
            val account = primaryAccount() ?: return
            val bundle = (peekBundle(account) ?: return).toMutableMap()
            if (bundle.remove(origin) == null) {
                return
            }
            if (bundle.isEmpty()) {
                // 最后一个 origin 也走了：账号本身删掉，系统账号页不留空壳（登出后"无残留账号"）
                accountManager.removeAccountExplicitly(account)
            } else {
                accountManager.setAuthToken(account, AUTH_TOKEN_TYPE, JsonSessionBundleCodec.encode(bundle))
            }
        } catch (e: RuntimeException) {
            Log.w(TAG, "账号库删除失败：origin=$origin, ${e.message}")
        }
    }

    override fun origins(): Set<String> {
        val result = LinkedHashSet<String>()
        peekBundle(primaryAccount())?.keys?.let { result.addAll(it) }
        // 还没被懒迁移的老账号也要算进来
        result.addAll(accountsOfType().map { it.name }.filter { it != accountName })
        return result
    }

    override fun clear() {
        accountsOfType().forEach { account ->
            try {
                accountManager.removeAccountExplicitly(account)
            } catch (e: RuntimeException) {
                Log.w(TAG, "账号库清空失败：${account.name}, ${e.message}")
            }
        }
    }

    /** 该 origin 在账号库里是否有凭据（一致性核对用）。 */
    fun accountExists(origin: String): Boolean = origin in origins()

    // ---- 账号库内部接线（全部吞异常，失败返回空/false，不打扰请求链路） ----

    private fun accountsOfType(): List<Account> = try {
        // 注意：老 API 的 getAccountsByType 返回数组，不是 List
        accountManager.getAccountsByType(accountType)?.toList() ?: emptyList()
    } catch (e: RuntimeException) {
        Log.w(TAG, "账号库枚举账号失败：${e.message}")
        emptyList()
    }

    private fun primaryAccount(): Account? = accountsOfType().firstOrNull { it.name == accountName }

    private fun legacyAccount(origin: String): Account? =
        accountsOfType().firstOrNull { it.name == origin }

    /** 读单账号里的凭据包；`null` = 读失败/坏数据（写路径据此放弃写入）。 */
    private fun peekBundle(account: Account?): Map<String, List<HttpCookie>>? {
        if (account == null) {
            return emptyMap()
        }
        val token = try {
            accountManager.peekAuthToken(account, AUTH_TOKEN_TYPE)
        } catch (e: RuntimeException) {
            Log.w(TAG, "账号库读取凭据失败：${e.message}")
            return null
        }
        if (!SessionBackendPolicy.hasUsableToken(token)) {
            return emptyMap()
        }
        return JsonSessionBundleCodec.decode(token)
    }

    /** 读老账号（单 origin 数组格式）。 */
    private fun peekLegacy(account: Account): List<HttpCookie> = try {
        val token = accountManager.peekAuthToken(account, AUTH_TOKEN_TYPE)
        if (!SessionBackendPolicy.hasUsableToken(token)) emptyList() else JsonCookieCodec.decode(token)
    } catch (e: RuntimeException) {
        Log.w(TAG, "老账号读取失败：${account.name}, ${e.message}")
        emptyList()
    }

    /** 把 [origin] 的凭据并入单账号；读回校验一致才删老账号。 */
    private fun mergeIntoBundle(origin: String, cookies: List<HttpCookie>): Boolean {
        val current = peekBundle(primaryAccount()) ?: return false
        if (!writeBundle(current.toMutableMap().apply { put(origin, cookies) }, origin)) {
            return false
        }
        val primary = primaryAccount() ?: return false
        val readBack = peekBundle(primary)?.get(origin).orEmpty()
        if (!equivalentCookies(cookies, readBack)) {
            Log.w(TAG, "并入后读回不一致，保留老账号：origin=$origin")
            return false
        }
        legacyAccount(origin)?.let { accountManager.removeAccountExplicitly(it) }
        Log.i(TAG, "老账号已并入单账号：origin=$origin")
        return true
    }

    /** 写整包（含大小上限判定与账号创建）。 */
    private fun writeBundle(bundle: Map<String, List<HttpCookie>>, origin: String): Boolean {
        val encoded = JsonSessionBundleCodec.encode(bundle)
        if (!SessionBackendPolicy.canStoreAuthToken(encoded)) {
            Log.w(TAG, "凭据包过长(${encoded.length})，按降级策略改存私有存储：origin=$origin")
            return false
        }
        return try {
            val account = ensurePrimaryAccount() ?: run {
                Log.w(TAG, "账号库账号不可用，降级私有存储：origin=$origin")
                return false
            }
            accountManager.setAuthToken(account, AUTH_TOKEN_TYPE, encoded)
            accountManager.setUserData(account, USER_DATA_SCHEMA_VERSION, JsonSessionBundleCodec.SCHEMA_VERSION)
            legacyAccount(origin)?.let { accountManager.removeAccountExplicitly(it) }
            true
        } catch (e: RuntimeException) {
            Log.w(TAG, "账号库写入失败，降级私有存储：origin=$origin, ${e.message}")
            false
        }
    }

    private fun ensurePrimaryAccount(): Account? {
        primaryAccount()?.let { return it }
        val account = Account(accountName, accountType)
        return if (accountManager.addAccountExplicitly(account, null, null)) {
            account
        } else {
            primaryAccount()
        }
    }
}

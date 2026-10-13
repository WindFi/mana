package me.sunzheng.mana.account.auth

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Singleton
import me.sunzheng.mana.utils.PreferenceManager



/**
 * 登出语义的**唯一入口**（design D7）：无论是 App 内主动登出，还是用户从「系统设置 → 账号」删除账号，
 * 都走 [clearSession]，保证「凭据被清除 + 判据转为未登录」同时发生。
 *
 * 清理覆盖四处（缺一处就会出现「系统里已删、本次进程仍当已登录」或「界面看起来已登录但请求 401」）：
 * 1. **账号库 + SP 残留**：`store.delete(origin)`（[MigratingSessionStore] 两端都清）；
 * 2. **内存**：`cookieStore.clearForOrigin(origin)` —— 否则本次进程仍会把 cookie 塞进请求；
 * 3. **判据**：清 `global` SP 的 `username`/`password`/`rememberd`（沿用改造前的键）；
 * 4. 判据的读取方（`AccountViewModel`）随之得到未登录，`LoginActivity` 重建 NavHost 回到 `nav_login`。
 *
 * [clearSession] 必须**幂等**：先主动登出、随后又收到账号库删除回调，不应抛错也不应产生重复副作用。
 */
class SessionLogout(
    private val globalPrefs: SharedPreferences,
    private val store: SessionStore,
    private val cookieStore: PersistentHttpCookieStore
) {

    /**
     * 清除该 origin 的会话，并返回「确实清掉了东西」（用于外部监听决定是否需要通知界面刷新）。
     *
     * [origin] 为 null（例如还没配 host、解析不出 host）时只清判据 —— 此时没有任何凭据可归属。
     * 账号已不存在、凭据已为空时也返回 false 而不抛错 —— 幂等要求。
     */
    fun clearSession(origin: String?): Boolean {
        val hadCredential = origin != null && store.read(origin).isNotEmpty()
        val hadLoginFlags = hasLoginFlags()

        if (origin != null) {
            cookieStore.clearForOrigin(origin)
            store.delete(origin)
        }
        clearLoginFlags()

        return hadCredential || hadLoginFlags
    }

    /**
     * 启动时一次核对（design D9，任务 4.3）：覆盖「App 未运行时账号被删」。
     *
     * 会先经 [SessionStore.read] 触发一次性迁移（design D10：**先迁移、后判据**），
     * 因此升级用户（SP 有凭据、账号库为空）会先搬迁成功、不会被误判为未登录。
     *
     * @return 该 origin 是否仍有可用凭据（登录态判据的唯一来源，供调用方直接使用，避免重复读取）
     */
    fun verifyOnStartup(origin: String): Boolean {
        val usable = store.hasUsableCredential(origin)
        if (!usable) {
            clearSession(origin)
        }
        return usable
    }

    private fun hasLoginFlags(): Boolean =
        globalPrefs.getBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, false) ||
            !globalPrefs.getString(PreferenceManager.Global.STR_USERNAME, "").isNullOrEmpty() ||
            !globalPrefs.getString(PreferenceManager.Global.STR_PASSWORD, "").isNullOrEmpty()

    private fun clearLoginFlags() {
        globalPrefs.edit()
            .putBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, false)
            .putString(PreferenceManager.Global.STR_USERNAME, "")
            .putString(PreferenceManager.Global.STR_PASSWORD, "")
            .apply()
    }
}



/**
 * 「本进程的会话状态被外部清掉了」的通知通道（design D5/D9）。
 *
 * 系统设置里删除账号时，存储层与本地登录态标志会被清掉，但已经创建的 `AccountViewModel`
 * 不会自动重算判据；这里用一个只会递增的版本号把「请重新判一次登录态」广播给它，
 * 从而让界面回到登录页（而不是停在「看起来已登录、请求 401」）。
 *
 * 只在**外部删除**（账号库监听）时由调用方触发；启动核对与主动登出由调用方自己刷新判据，
 * 避免同一轮里重复触发。
 */
@Singleton
class SessionStateHolder @Inject constructor() {

    private val _revision = MutableLiveData(0L)

    /** 版本号；每次外部会话清理 +1。 */
    val revision: LiveData<Long> get() = _revision

    fun notifySessionCleared() {
        _revision.postValue((_revision.value ?: 0L) + 1L)
    }
}


/** 账号库账号的纯数据投影，避免把 `android.accounts.Account` 带进可单测的判定逻辑。 */
internal data class AccountKey(val name: String, val type: String)

/**
 * 「系统侧删除账号 ⇒ 视为登出」的**纯判定**（design D5/D9，任务 4.2）。
 *
 * 删除账号不会回调 authenticator，只能靠 `addOnAccountsUpdatedListener`（App 运行中）或启动核对（App 未运行）发现。
 * 监听回调给的是**全量**账号列表，因此这里做「上次已知的本 App origin − 当前本 App origin」的差集。
 */
/** 当前列表里属于本 App accountType 的账号名（即 origin）。 */
internal fun mineAccounts(current: Collection<AccountKey>, accountType: String): Set<String> =
    current.asSequence()
        .filter { it.type == accountType }
        .map { it.name }
        .toSet()

/**
 * 需要清理的 origin：上次已知、且本次已消失的本 App 账号。
 *
 * 其它 accountType（其它 App/其它类型）的账号一律不参与判定，因此不会误伤。
 */
internal fun originsToClear(
    knownMine: Set<String>,
    current: Collection<AccountKey>,
    accountType: String
): Set<String> = knownMine - mineAccounts(current, accountType)


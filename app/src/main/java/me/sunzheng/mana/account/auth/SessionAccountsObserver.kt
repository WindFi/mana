package me.sunzheng.mana.account.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.OnAccountsUpdateListener
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.startup.Initializer
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import javax.inject.Inject
import javax.inject.Singleton
import me.sunzheng.mana.R



/**
 * 「用户在系统设置里删除 mana 账号」⇒ 视为登出（design D5/D9，任务 4.2）。
 *
 * 账号被删除**不会回调 authenticator**，只能靠这个监听发现（App 未运行时的删除由启动核对兜底）。
 * 回调只做一件事：把「本 App accountType 的账号从上次快照里消失」的那些 origin 交给 [SessionLogout] 清理，
 * 并让 [SessionStateHolder] 通知界面重算登录态（回到登录页）。
 *
 * 整个判定是纯函数 [originsToClear]，这层只负责 Android 接线。
 * 回调跑在独立的 HandlerThread 上，避免在系统账号变更时占用主线程（清理里有账号库/SP 写入）。
 *
 * 注册时机：由 [SessionAccountsInitializer]（AndroidX App Startup）在进程启动时触发；
 * 这样 `account` 自己在清单里"自报家门"，`utils` 不需要认识 `account`（workdoc `011-utils` §2 红线）。
 */
@Singleton
class SessionAccountsObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionLogout: SessionLogout,
    private val sessionState: SessionStateHolder
) : OnAccountsUpdateListener {

    companion object {
        private const val TAG = "SessionStore"
    }

    /** 与 authenticator.xml 声明的一致（同一个字符串资源，debug 变体在 src/debug/res 覆盖）。 */
    private val accountType: String = context.getString(R.string.session_account_type)

    /** 上次已知的本 App origin（账号名）。只在监听线程上读写。 */
    private val knownOrigins = HashSet<String>()

    private val handlerThread: HandlerThread by lazy {
        HandlerThread("mana-session-accounts").apply { start() }
    }

    private var started = false

    /** 账号库后端启用时才注册；幂等（App Startup 只会调一次，这里再兜一层）。 */
    @Synchronized
    fun start() {
        if (started) {
            return
        }
        val accountManager = AccountManager.get(context)
        if (!SessionBackendPolicy.useAccountManager(Build.VERSION.SDK_INT, accountManager != null)) {
            Log.i(TAG, "账号库后端未启用（sdk=${Build.VERSION.SDK_INT}），不注册账号变更监听")
            return
        }
        accountManager.addOnAccountsUpdatedListener(this, Handler(handlerThread.looper), true)
        started = true
        Log.i(TAG, "已注册账号变更监听：accountType=$accountType")
    }

    override fun onAccountsUpdated(accounts: Array<out Account>?) {
        val current = accounts.orEmpty().map { AccountKey(it.name, it.type) }
        val toClear = originsToClear(knownOrigins, current, accountType)
        knownOrigins.clear()
        knownOrigins.addAll(mineAccounts(current, accountType))

        for (origin in toClear) {
            val cleared = sessionLogout.clearSession(origin)
            Log.i(TAG, "系统侧删除账号 ⇒ 清理本机会话：origin=$origin, cleared=$cleared")
            if (cleared) {
                sessionState.notifySessionCleared()
            }
        }
    }
}



/**
 * 进程启动的挂载点（AndroidX App Startup，`androidx.startup:startup-runtime`）。
 *
 * 为什么需要它：`SessionAccountsObserver` 必须在进程启动时就注册 `addOnAccountsUpdatedListener`
 * —— 账号被用户从系统设置删除时**不会回调 authenticator**，只有这个监听能立刻发现（openspec change
 * `accountmanager-session-store` design D9）。但注册点在 `Application` 里，而 `Application` 位于
 * `utils`，`utils` 不允许 import `account`（workdoc `011-utils-通用工具与全局配置.md` §2 红线）。
 *
 * 用 AndroidX App Startup 后**双方都不用认识对方**：`account` 在这里自报家门，
 * 由库自带的 `androidx.startup.InitializationProvider`（`ContentProvider`，早于 `Application.onCreate`）
 * 触发；`utils`/`App` 零改动。清单里的 meta-data 见 `AndroidManifest.xml`。
 *
 * 注意：这里通过 Hilt `@EntryPoint` 取**单例** `SessionAccountsObserver`，而不是自己 new 一个 ——
 * 监听清理时必须落到 App 正在用的那套 `SessionStore`/`PersistentHttpCookieStore` 上。
 */
class SessionAccountsInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        EntryPointAccessors.fromApplication(context, SessionStartupEntryPoint::class.java)
            .sessionAccountsObserver()
            .start()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SessionStartupEntryPoint {
        fun sessionAccountsObserver(): SessionAccountsObserver
    }
}


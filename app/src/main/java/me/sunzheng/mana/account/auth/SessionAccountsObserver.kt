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
 * 回调做两件事：
 * 1. 判定「本 App 的**账号标识**（账号名 + 账号类型）从上次快照里消失」——纯函数 [ownAccountDisappeared]；
 *    **账号名不是 origin**，因此绝不拿账号名去当凭据键（design D11）；
 * 2. 命中后不立即清理，交给 [AccountDeletionConfirmer] 延迟复核（迁移自身写账号库会产生瞬态快照，
 *    立即清理会把刚迁好的账号连凭据一起清掉，design D13）；复核确认才走 [SessionLogout.clearAllSessions]
 *    （单一入口：清内存、账号库、私有存储全部 origin 与判据），并让 [SessionStateHolder] 通知界面回登录页。
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

        /** 收到「账号消失」后延迟多久复核（design D13）；平台一次删除可能连发多个快照。 */
        private const val RECHECK_DELAY_MS = 400L
    }

    /** 与 authenticator.xml 声明的一致（同一个字符串资源，debug 变体在 src/debug/res 覆盖）。 */
    private val accountType: String = context.getString(R.string.session_account_type)

    /** 复核读不到账号时的占位：非空 ⇒ 按「账号仍在」处理（宁可漏清，不可误清）。 */
    private val unreadable = setOf("<unreadable>")

    /** 上次已知的**本 App 账号标识**（账号名 + 账号类型，design D11）。只在监听线程上读写。 */
    private val knownAccounts = HashSet<AccountKey>()

    /**
     * 删除事件发生**之前**读到的 origin 快照。
     *
     * 为什么需要：账号被删除后平台才回调，回调里读账号 authToken 一定是空的（真机实测 origins=[]），
     * 所以"清掉了哪些 origin"只能靠删除前那次快照（design D14 的日志证据）。
     */
    private var knownOrigins: Set<String> = emptySet()

    private val handlerThread: HandlerThread by lazy {
        HandlerThread("mana-session-accounts").apply { start() }
    }

    private val handler: Handler by lazy { Handler(handlerThread.looper) }

    /**
     * 「账号标识消失 ⇒（延迟复核后）清空全部会话」（design D13）。
     *
     * 为什么必须复核：写账号库的人是我们自己 —— 迁移/并入老账号时的
     * `addAccountExplicitly` / `removeAccountExplicitly` 会回调出「本 App 账号暂时为空」的瞬态快照，
     * 立即清理会把刚迁好的账号与凭据一起清掉。
     */
    private val deletionConfirmer = AccountDeletionConfirmer(
        recheckDelayMs = RECHECK_DELAY_MS,
        scheduleRecheck = { delayMs, action -> handler.postDelayed(action, delayMs) },
        accountsOfOurType = { ourAccountNamesNow() },
        onConfirmedDeletion = { clearEverything() }
    )

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
        accountManager.addOnAccountsUpdatedListener(this, handler, true)
        started = true
        Log.i(TAG, "已注册账号变更监听：accountType=$accountType")
    }

    override fun onAccountsUpdated(accounts: Array<out Account>?) {
        val current = accounts.orEmpty().map { AccountKey(it.name, it.type) }
        val disappeared = ownAccountDisappeared(knownAccounts, current, accountType)
        knownAccounts.clear()
        knownAccounts.addAll(ourAccounts(current, accountType))

        if (disappeared) {
            Log.i(
                TAG,
                "本 App 账号标识消失（accountType=$accountType）⇒ ${RECHECK_DELAY_MS}ms 后复核" +
                    "；删除前快照 origins=$knownOrigins"
            )
            deletionConfirmer.onAccountDisappeared()
        } else {
            // 账号还在：更新"删除前快照"（非空才覆盖，避免瞬态空快照把记录抹掉）。
            sessionLogout.currentOrigins().takeIf { it.isNotEmpty() }?.let { knownOrigins = it }
        }
    }

    /** 复核用：**直接问系统**当前属于本 App 类型的账号名（不信任回调快照）。 */
    private fun ourAccountNamesNow(): Set<String> = try {
        AccountManager.get(context).getAccountsByType(accountType).orEmpty()
            .map { it.name }
            .toSet()
    } catch (e: RuntimeException) {
        Log.w(TAG, "复核读取账号失败，按「账号仍在」处理：${e.message}")
        unreadable
    }

    /**
     * 复核确认「账号真的没了」⇒ 清空本 App 全部会话（design D12 的单一入口）。
     *
     * 本 App 只有一个账号，因此账号消失等价于**全部 origin 登出**；日志里带上被清掉的 origin，
     * 验收时不必再翻存储（design D14）。
     */
    private fun clearEverything() {
        val clearedOrigins = sessionLogout.clearAllSessions(knownOrigins)
        Log.i(TAG, "系统侧删除账号 ⇒ 清空本 App 全部会话：origins=$clearedOrigins")
        sessionState.notifySessionCleared()
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

/**
 * 「本 App 账号标识消失 ⇒ 清理」的**去抖 + 复核**（openspec change `fix-session-store-defects` design D13）。
 *
 * 为什么不能收到回调就清：写账号库的人就是我们自己 —— 迁移/并入老账号时会
 * `addAccountExplicitly`（建单账号）与 `removeAccountExplicitly`（清掉旧模型的同名 origin 账号），
 * 每个动作都会回调监听器，监听器可能看到「本 App 账号暂时为空」的**瞬态快照**。
 * 若立刻清理，会把刚迁好的账号连凭据一起清掉（升级用户掉登录 + 账号库被清空的形态）。
 *
 * 因此：收到「消失」→ 延迟 [recheckDelayMs] → 向系统**复核**一次 → 仍然为空才清理。
 * 复核读不到账号时调用方必须按「账号仍在」处理（宁可漏清，不可误清）。
 *
 * 不持有线程/Handler：调度与"当前账号"都由调用方注入，因此可在纯 JVM 单测里同步驱动
 * （见 `AccountDeletionConfirmerTest`）。
 */
class AccountDeletionConfirmer(
    private val recheckDelayMs: Long,
    private val scheduleRecheck: (delayMs: Long, action: () -> Unit) -> Unit,
    private val accountsOfOurType: () -> Set<String>,
    private val onConfirmedDeletion: () -> Unit
) {

    private val lock = Any()
    private var pending = false

    /** 收到一次「账号标识消失」；排队期间重复调用只算一次（平台一次删除可能连发多个快照）。 */
    fun onAccountDisappeared() {
        synchronized(lock) {
            if (pending) {
                return
            }
            pending = true
        }
        scheduleRecheck(recheckDelayMs) { recheck() }
    }

    private fun recheck() {
        // 先复位 pending：本轮结束（无论是否清理），下一轮「消失」都必须能再次触发。
        synchronized(lock) { pending = false }
        val remaining = accountsOfOurType()
        if (remaining.isEmpty()) {
            onConfirmedDeletion()
        }
    }
}

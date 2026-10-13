package me.sunzheng.mana.account.auth

import android.accounts.AccountManager
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.sunzheng.mana.R
import me.sunzheng.mana.utils.PreferenceManager
import javax.inject.Singleton

/**
 * 会话凭据存储的装配点（design D6，任务 1.3/2.3/2.4）。
 *
 * 后端选择：`SDK_INT >= 23` 且账号库可用 ⇒ 账号库（主）+ SP（兜底/迁移源）；否则只用 SP。
 * 这里只做选择与接线，判定逻辑在纯函数 [SessionBackendPolicy] 里（有单测），
 * 因此低版本/账号库不可用的分支也能被自动覆盖。
 */
@Module
@InstallIn(SingletonComponent::class)
object SessionModule {

    private const val TAG = "SessionStore"

    @Singleton
    @Provides
    fun provideSessionStore(@ApplicationContext context: Context): SessionStore {
        val legacy = SharedPreferencesSessionStore(
            context.getSharedPreferences(SharedPreferencesSessionStore.PREFS_NAME, Context.MODE_PRIVATE)
        )
        val accountManager = AccountManager.get(context)
        if (!SessionBackendPolicy.useAccountManager(Build.VERSION.SDK_INT, accountManager != null)) {
            Log.i(
                TAG,
                "会话凭据后端 = 私有存储（sdk=${Build.VERSION.SDK_INT}, 账号库可用=${accountManager != null}）"
            )
            return legacy
        }

        // accountType 的唯一真源是 authenticator.xml 引用的那个字符串资源（系统只认它声明的东西）。
        val accountType = context.getString(R.string.session_account_type)
        val derived = SessionBackendPolicy.accountTypeOf(context.packageName)
        if (accountType != derived) {
            // 声明值与 <applicationId>.session 不一致时账号库写入会失败（随后降级 SP），这里让它可见。
            Log.e(TAG, "authenticator.xml 声明的 accountType($accountType) 与 <applicationId>.session($derived) 不一致")
        }
        // 单账号：名字只用于显示（系统「设置 → 账号」那一行），所以取 App 名；
        // 查找按 accountType，改 App 名不会丢账号。
        val accountStore = AccountManagerSessionStore(
            accountManager!!,
            accountType,
            context.getString(R.string.app_name)
        )
        Log.i(TAG, "会话凭据后端 = 系统账号库（accountType=$accountType）")
        // 主后端（账号库）+ 兜底后端（SP）+ 一次性迁移，合成一个类（2026-10-12 归集）。
        return MigratingSessionStore(accountStore, legacy) { message -> Log.w(TAG, message) }
    }

    @Singleton
    @Provides
    fun providePersistentHttpCookieStore(sessionStore: SessionStore): PersistentHttpCookieStore =
        PersistentHttpCookieStore(sessionStore)

    @Singleton
    @Provides
    fun provideSessionLogout(
        @ApplicationContext context: Context,
        sessionStore: SessionStore,
        cookieStore: PersistentHttpCookieStore
    ): SessionLogout = SessionLogout(
        globalPrefs = context.getSharedPreferences(
            PreferenceManager.Global.STR_SP_NAME,
            Context.MODE_PRIVATE
        ),
        store = sessionStore,
        cookieStore = cookieStore
    )
}

package me.sunzheng.mana.account.auth

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.accounts.NetworkErrorException
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.util.Log
import me.sunzheng.mana.R



/**
 * 本 App 账号类型的 authenticator（design D3）。
 *
 * 这是 `exported` Service 暴露出去的一层，所以：
 * - **必须校验调用方**：只允许本进程（`peekAuthToken` 的调用方）与 system 使用；同 accountType 的第三方应用一律拒绝；
 * - `getAuthToken` **只回本地已存值**：不弹界面、不发网络、取不到返回 null —— 否则请求链路会被它拖死，
 *   这也正是不能用 `blockingGetAuthToken` 的原因；
 * - 其余方法（`addAccount`/`updateCredentials`/…）一律返回「不支持」：本 App 的登录界面是自己的，
 *   不接入系统的账号添加流程（design Non-Goals）。
 */
class SessionAuthenticator(
    private val context: Context,
    private val accountManager: AccountManager,
    private val accountType: String
) : AbstractAccountAuthenticator(context) {

    companion object {
        private const val TAG = "SessionAuthenticator"
    }

    override fun getAuthToken(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle? {
        if (account == null || authTokenType != AccountManagerSessionStore.AUTH_TOKEN_TYPE) {
            return null
        }
        if (!isCallerAllowed()) {
            Log.w(TAG, "拒绝非本应用调用 getAuthToken（uid=${Binder.getCallingUid()}）")
            return null
        }
        val token = try {
            accountManager.peekAuthToken(account, authTokenType)
        } catch (e: RuntimeException) {
            Log.w(TAG, "读取本地凭据失败：${e.message}")
            null
        }
        if (token.isNullOrEmpty()) {
            return null
        }
        return Bundle().apply { putString(AccountManager.KEY_AUTHTOKEN, token) }
    }

    override fun getAuthTokenLabel(authTokenType: String?): String? =
        if (authTokenType == AccountManagerSessionStore.AUTH_TOKEN_TYPE) {
            "$accountType.${AccountManagerSessionStore.AUTH_TOKEN_TYPE}"
        } else {
            null
        }

    override fun addAccount(
        response: AccountAuthenticatorResponse?,
        accountType: String?,
        authTokenType: String?,
        requiredFeatures: Array<out String>?,
        options: Bundle?
    ): Bundle = unsupported()

    override fun updateCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        authTokenType: String?,
        options: Bundle?
    ): Bundle = unsupported()

    override fun confirmCredentials(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        options: Bundle?
    ): Bundle? = null

    override fun editProperties(
        response: AccountAuthenticatorResponse?,
        accountType: String?
    ): Bundle? = null

    override fun hasFeatures(
        response: AccountAuthenticatorResponse?,
        account: Account?,
        features: Array<out String>?
    ): Bundle = Bundle().apply { putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false) }

    /** 只信任本应用进程与 system_server；同 accountType 的其它应用会拿到 null。 */
    private fun isCallerAllowed(): Boolean {
        val callingUid = Binder.getCallingUid()
        if (callingUid == Process.myUid() || callingUid == Process.SYSTEM_UID) {
            return true
        }
        val caller = try {
            context.packageManager.getNameForUid(callingUid)
        } catch (e: RuntimeException) {
            null
        }
        Log.w(TAG, "调用方不可信：uid=$callingUid, package=$caller, accountType=$accountType")
        return false
    }

    private fun unsupported(): Bundle = Bundle().apply {
        putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION)
        putString(AccountManager.KEY_ERROR_MESSAGE, "mana 不支持系统账号添加流程")
    }
}



/**
 * 账号库要求注册的 exported Service（design D2/D3，任务 2.3）。
 *
 * 只在 API ≥ 23 才会真正被系统使用（低版本走 SP 后端，且不需要任何账号权限）。
 * `accountType` 直接读 authenticator.xml 引用的同一个字符串资源（`R.string.session_account_type`），
 * 保证「系统登记的 authenticator」与「我们写的账号」永远是同一个类型。
 */
class SessionAuthenticatorService : Service() {

    private lateinit var authenticator: SessionAuthenticator

    override fun onCreate() {
        super.onCreate()
        authenticator = SessionAuthenticator(
            context = this,
            accountManager = AccountManager.get(this),
            accountType = getString(R.string.session_account_type)
        )
    }

    override fun onBind(intent: Intent?): IBinder = authenticator.iBinder
}


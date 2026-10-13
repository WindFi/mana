package me.sunzheng.mana.account

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.NetworkBoundResource
import me.sunzheng.mana.core.net.v2.SignInRequest
import me.sunzheng.mana.home.bangumi.DefaultResponse
import me.sunzheng.mana.utils.PreferenceManager
import me.sunzheng.mana.account.auth.SessionLogout
import me.sunzheng.mana.account.auth.SessionOrigin
import me.sunzheng.mana.account.auth.SessionStateHolder
import me.sunzheng.mana.account.auth.SessionStore
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val sessionStore: SessionStore,
    private val sessionLogout: SessionLogout,
    private val sessionState: SessionStateHolder
) : ViewModel() {


    val isLoginLiveData = MutableLiveData<Boolean>()

    @Inject
    lateinit var repository: Lazy<AccountRepository>

    /**
     * 外部（系统设置里删除账号）把会话清掉后重新判据，让界面回到登录页而不是停在
     * 「看起来已登录、请求却 401」。主动登出与启动核对由调用方自己刷新，不走这里。
     */
    private val sessionClearedObserver = Observer<Long> { checkIsLogin(appContext) }

    init {
        sessionState.revision.observeForever(sessionClearedObserver)
    }

    override fun onCleared() {
        sessionState.revision.removeObserver(sessionClearedObserver)
        super.onCleared()
    }

    /**
     * 登录态判据 = 该 origin 是否存在**可用会话凭据**（openspec `accountmanager-session-store` design D8），
     * 不再看 SP 里的用户名/密码是否非空。
     *
     * 顺序（design D10：先迁移、后判据）：[SessionLogout.verifyOnStartup] 先触发一次性迁移并做启动核对
     * （覆盖「App 未运行时账号被删」），其返回值即判据，避免重复读取。
     *
     * 保持同步：账号库下用的是 `peekAuthToken`（官方标注主线程安全、不会回调 authenticator、不触网）。
     */
    fun checkIsLogin(context: Context) {
        val origin = SessionOrigin.keyOfHost(hostOf(context))
        if (origin == null) {
            publish(false)
            return
        }
        publish(sessionLogout.verifyOnStartup(origin))
    }

    private fun hostOf(context: Context): String =
        context.getSharedPreferences(
            PreferenceManager.Global.STR_SP_NAME,
            Context.MODE_PRIVATE
        ).getString(PreferenceManager.Global.STR_KEY_HOST, "") ?: ""

    /** 只在结果变化时发值：`LoginActivity` 的观察者会重建 NavHost，避免无谓重建。 */
    private fun publish(isLogin: Boolean) {
        if (isLoginLiveData.value != isLogin) {
            isLoginLiveData.postValue(isLogin)
        }
    }

    fun siginIn(userName: String, password: String, isRembered: Boolean = false) =
        repository.get().signIn(userName, password, isRembered)
}

class AccountRepository {
    lateinit var apiService: ApiService
    fun signIn(userName: String, password: String, isRembered: Boolean) =
        object : NetworkBoundResource<String, DefaultResponse>() {
            override fun saveCallResult(item: DefaultResponse) {
                // TODO: 挖个坑 以后看有没有机会挪到这里面来
//            sharedPreferences!!.edit()
//                .putBoolean(
//                    PreferenceManager.Global.BOOL_IS_REMEMBERD,
//                    isRembered
//                )
//                .putString(
//                    PreferenceManager.Global.STR_USERNAME,
//                    userName
//                )
//                .putString(
//                    PreferenceManager.Global.STR_PASSWORD,
//                    password
//                )
//                .commit()
            }

            override fun shouldFetch(data: String?): Boolean = true

            override fun loadFromDb(): LiveData<String> {
                return MutableLiveData("ok")
            }

            override fun createCall() =
                apiService.login(SignInRequest(userName, password, isRembered))

        }.asLiveData()
}

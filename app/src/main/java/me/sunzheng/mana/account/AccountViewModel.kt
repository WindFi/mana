package me.sunzheng.mana.account

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.NetworkBoundResource
import me.sunzheng.mana.core.net.v2.SignInRequest
import me.sunzheng.mana.home.bangumi.DefaultResponse
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var apiService: ApiService

    val repository: AccountRepository by lazy {
        AccountRepository().apply {
            this.apiService = this@AccountViewModel.apiService
        }
    }

    fun siginIn(userName: String, password: String, isRembered: Boolean = false) =
        repository.signIn(userName, password, isRembered)

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
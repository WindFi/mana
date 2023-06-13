package me.sunzheng.mana

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.account.AccountViewModel
import me.sunzheng.mana.databinding.ActivityLoginBinding
import javax.inject.Inject

/**
 * A login screen that offers login via email/password.
 */
@AndroidEntryPoint
class LoginActivity @Inject constructor() : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    val viewModel by viewModels<AccountViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
        viewModel.isLoginLiveData.observe(this) {
            var id = when (it) {
                false -> {
                    R.navigation.nav_login
                }

                true -> {
                    R.navigation.nav_main
                }
            }
            var fragment = NavHostFragment.create(id)
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .setPrimaryNavigationFragment(fragment)
                .commit()
        }
        replace()
    }

    fun replace() {
        viewModel.checkIsLogin(this)
    }

    override fun onNavigateUp(): Boolean {
        val navController = findNavController(supportFragmentManager.primaryNavigationFragment!!.id)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    companion object {
        private val TAG = LoginActivity::class.java.simpleName
    }
}
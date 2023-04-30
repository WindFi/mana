package me.sunzheng.mana

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
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

    val viewModel = viewModels<AccountViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setSupportActionBar(binding.toolbar)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
//        var vn =ActivityNavigator(this)
//        navController.navigatorProvider += vn
        binding.fab.setOnClickListener { v ->
            // TODO:  implement it :  goto next
            when (navController.currentDestination?.id) {
                R.id.fragment_host -> {
                    navController.navigate(R.id.action_destination_host_to_login)
                }

                R.id.fragment_username -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
        }

    }

    fun onSave() {
//        controller!!.loginViewShow(
//            (applicationContext as App).retrofit.create(
//                AccountApiService.Login::class.java
//            )
//        )
    }

    override fun onNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    companion object {
        private val TAG = LoginActivity::class.java.simpleName
    }
}
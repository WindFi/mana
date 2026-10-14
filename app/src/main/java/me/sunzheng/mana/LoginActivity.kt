package me.sunzheng.mana

import android.os.Bundle
import java.lang.ref.WeakReference
import androidx.lifecycle.Observer
import android.util.Log
import android.content.Intent
import android.app.Application
import android.app.Activity
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.account.AccountViewModel
import me.sunzheng.mana.account.auth.SessionStateHolder
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

    /** 「会话被外部清掉就退出当前页面回登录页」的进程级协调器安装要用（design D16）。 */
    @Inject
    lateinit var sessionState: SessionStateHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 安装点选这里：LoginActivity 是 LAUNCHER，进程内必然创建过；这样 utils/App 不用认识 account。
        SessionExitCoordinator.install(application, sessionState)
        WindowCompat.enableEdgeToEdge(window)
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

/**
 * 「会话被外部清掉」⇒ **任意页面** 都必须离开并回到登录页（openspec change `fix-session-store-defects` design D16）。
 *
 * 为什么需要它：清空信号（[SessionStateHolder.revision]）原本只有 `AccountViewModel` 在听，而它驱动的是
 * `LoginActivity` 里的 NavHost。用户在**播放页 / 搜索页 / 收藏页 / 详情页 / 设置页**（都是独立 Activity）
 * 删除系统账号时，数据虽然清了，界面却停在原地 —— 尤其播放器会继续放音。
 *
 * 做法：进程级监听 [SessionStateHolder.revision]，一旦递增（= 会话被外部清掉）且当前页不是 [LoginActivity]，
 * 就 `NEW_TASK|CLEAR_TASK` 启动 [LoginActivity]：**整个返回栈被清掉**（播放器等页面一起 finish，
 * 播放器 `onDestroy` 会自行发 `ACTION_RELEASE`/`ACTION_STOP` ⇒ 播放随之停止），登录壳重新按判据渲染 `nav_login`。
 * 当前页就是 [LoginActivity] 时不动作，交给它自己的 NavHost 切换，避免闪一下重建。
 *
 * 落位说明：放在**包根**是因为它天然同时认识 [LoginActivity]（包根壳）与 `account.auth.SessionStateHolder`；
 * 放进 `account` 会逼 account 反向 import 包根壳类（workdoc `app-modules.md` §4 红线）。安装点是 [LoginActivity]
 * （LAUNCHER，进程内必然创建过），因此 `utils`/`Application` 依旧零改动、零 `account` import。
 *
 * 生命周期：协调器与进程同寿，`observeForever` 不解除（进程级单例，见 [install] 的幂等）。
 */
class SessionExitCoordinator private constructor(
    private val application: Application,
    private val sessionState: SessionStateHolder
) : Application.ActivityLifecycleCallbacks {

    /** 当前处于 resumed 的 Activity（弱引用，避免持有已销毁页面）。 */
    private var resumedActivity = WeakReference<Activity>(null)

    /** 上一次看到的版本号；`-1` 表示还没收到过回调。只在主线程读写。 */
    private var seenRevision = -1L

    private val revisionObserver = Observer<Long> { revision ->
        val previous = seenRevision
        seenRevision = revision
        onRevisionChanged(previous, revision)
    }

    private fun onRevisionChanged(previous: Long, current: Long) {
        if (!shouldRelocate(previous, current)) {
            return
        }
        val currentActivity = resumedActivity.get()
        if (currentActivity is LoginActivity) {
            Log.i(TAG, "会话被清空：当前已在登录壳，交给 NavHost 切回登录页")
            return
        }
        Log.i(
            TAG,
            "会话被清空 ⇒ 退出当前页面(${currentActivity?.javaClass?.simpleName})并回到登录页"
        )
        application.startActivity(
            Intent(application, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    override fun onActivityResumed(activity: Activity) {
        resumedActivity = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (resumedActivity.get() === activity) {
            resumedActivity.clear()
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (resumedActivity.get() === activity) {
            resumedActivity.clear()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    companion object {
        private const val TAG = "SessionStore"

        @Volatile
        private var installed: SessionExitCoordinator? = null

        /**
         * 幂等安装（由 [LoginActivity] 在 `onCreate` 调用一次）。
         *
         * `observeForever` 会**立刻回调当前值**（初始 `0`），那不是「刚被清掉」的事件，因此
         * [shouldRelocate] 会跳过第一次投递。
         */
        fun install(
            application: Application,
            sessionState: SessionStateHolder
        ): SessionExitCoordinator {
            installed?.let { return it }
            synchronized(this) {
                installed?.let { return it }
                val coordinator = SessionExitCoordinator(application, sessionState)
                application.registerActivityLifecycleCallbacks(coordinator)
                sessionState.revision.observeForever(coordinator.revisionObserver)
                installed = coordinator
                Log.i(TAG, "已安装会话退出协调器：会话被清空后任意页面都会退出并回到登录页")
                return coordinator
            }
        }
    }
}

/**
 * 该版本号变化是否代表「会话刚被外部清掉」。
 *
 * - 第一次投递（`previous < 0`，LiveData 的初始值）**不算**事件，否则安装即误判并弹出登录页；
 * - 只有**递增**才算（`clearAllSessions` 每次 `postValue` 都会 +1）。
 */
internal fun shouldRelocate(previous: Long, current: Long): Boolean =
    previous >= 0L && current > previous
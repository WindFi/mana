package me.sunzheng.mana.utils

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import androidx.core.os.LocaleListCompat
import androidx.multidex.MultiDexApplication
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import dagger.hilt.android.HiltAndroidApp
import me.sunzheng.mana.R
import me.sunzheng.mana.ThemeMode
import me.sunzheng.mana.setAppCompatDelegateTheme
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Created by Sun on 2017/5/21.
 */
@HiltAndroidApp
class App : MultiDexApplication() {
    val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, MODE_PRIVATE).apply {
            registerOnSharedPreferenceChangeListener(configListener)
        }
    }
    var isConfigChanged = false
    val configListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == PreferenceManager.Global.STR_KEY_HOST) {
            isConfigChanged = true
        }
    }

    override fun onCreate() {
        super.onCreate()
        Glide.get(this).setMemoryCategory(MemoryCategory.NORMAL)
        initGlobalPreferences()

        var m = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this@App)
            .getString(getString(R.string.pref_key_dark_theme), "0")
        var model = when (m) {
            "0" -> ThemeMode.LIGHT
            "1" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        setAppCompatDelegateTheme(model)
    }

    private val host: String?
        private get() = sharedPreferences.getString(PreferenceManager.Global.STR_KEY_HOST, "")


    private fun defaultOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10000, TimeUnit.MILLISECONDS)
            .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .addInterceptor { chain ->
                var request = chain.request()
                var headers = request.headers()
                var s = headers["User-Agent"]
                if (TextUtils.isEmpty(s)) {
                    s = userAgent
                } else {
                    s += ";$userAgent"
                }
                headers = headers.newBuilder().set("User-Agent", s).build()
                request = request.newBuilder().headers(headers).build()
                chain.proceed(request)
            }
            .cookieJar(
                JavaNetCookieJar(
                    CookieManager(
                        PersistentHttpCookieStore(this),
                        CookiePolicy.ACCEPT_ALL
                    )
                )
            )
            .build()
    }

    val userAgent: String
        get() = "$applicationLabel/$versionCode"
    val versionCode: String
        get() {
            try {
                val packageInfo = packageManager.getPackageInfo(packageName, 0)
                return packageInfo.versionCode.toString()
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            return "0"
        }
    val applicationLabel: String
        get() = applicationInfo.loadLabel(packageManager).toString()
    val language: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleListCompat.getDefault()[0]!!.language
        } else Locale.getDefault().language

    fun initGlobalPreferences() {
//        ja first
        val __sharedPreferences =
            android.preference.PreferenceManager.getDefaultSharedPreferences(this)
        if (!__sharedPreferences.contains(getString(PreferenceManager.Global.RES_JA_FIRST_BOOL))) {
            val displayLanguage = language
            val isJaLanguage = displayLanguage.lowercase(Locale.getDefault()).contains("ja")
            __sharedPreferences.edit()
                .putBoolean(getString(PreferenceManager.Global.RES_JA_FIRST_BOOL), isJaLanguage)
                .commit()
        }
    }
}
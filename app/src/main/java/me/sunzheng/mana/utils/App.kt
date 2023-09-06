package me.sunzheng.mana.utils

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import androidx.core.os.LocaleListCompat
import androidx.multidex.MultiDexApplication
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import dagger.hilt.android.HiltAndroidApp
import me.sunzheng.mana.R
import me.sunzheng.mana.ThemeMode
import me.sunzheng.mana.setAppCompatDelegateTheme
import java.util.Locale

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
    val configListener = OnSharedPreferenceChangeListener { _, key ->
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
package me.sunzheng.mana

import androidx.appcompat.app.AppCompatDelegate

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
}

fun setAppCompatDelegateTheme(themeMode: ThemeMode) {
    AppCompatDelegate.setDefaultNightMode(
        when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        },
    )
}
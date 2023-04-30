package me.sunzheng.mana

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import me.sunzheng.mana.MainActivity
import me.sunzheng.mana.utils.PreferenceManager

class LaunchActivity : Activity() {
    lateinit var sharedPreferences: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, MODE_PRIVATE)
        var intent: Intent? = if (sharedPreferences.getBoolean(
                PreferenceManager.Global.BOOL_IS_REMEMBERD,
                false
            )
        ) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
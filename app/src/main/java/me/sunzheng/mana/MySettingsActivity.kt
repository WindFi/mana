package me.sunzheng.mana

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import me.sunzheng.mana.databinding.ActivityMySettingsBinding

class MySettingsActivity : AppCompatActivity() {
    val binding: ActivityMySettingsBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_my_settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, GeneralSettingsFragment())
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            onBackPressedDispatcher.onBackPressed()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_settings, rootKey)
        preferenceScreen.findPreference<ListPreference>(getString(R.string.pref_key_dark_theme))
            ?.apply {
                this.summary = this.entries[this.value.toString().toInt()]
            }?.setOnPreferenceChangeListener { preference, newValue ->
                if (preference is ListPreference)
                    preference.summary = preference.entries[newValue.toString().toInt()]
                var model = when (newValue.toString()) {
                    "0" -> ThemeMode.LIGHT
                    "1" -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
                setAppCompatDelegateTheme(model)
                true
            }
    }
}
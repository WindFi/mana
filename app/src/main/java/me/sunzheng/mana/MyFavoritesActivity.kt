package me.sunzheng.mana

import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.databinding.ActivityMyFavioursBinding
import me.sunzheng.mana.home.mybangumi.FavriouteFragmentCompat
import javax.inject.Inject

/**
 * MyFaviours activity
 * for list the faviours
 * and click any item goto [BangumiDetailsActivity]
 */
@AndroidEntryPoint
class MyFavoritesActivity @Inject constructor() : AppCompatActivity() {
    companion object {
        @JvmStatic
        val KEY_POSITION_INT = "${MyFavoritesActivity::class.java.simpleName}_position"
    }

    val binding: ActivityMyFavioursBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_my_faviours)
    }
    val preference: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        var position = preference.getInt("KEY_POSITION_INT", 0)
        binding.spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View,
                position: Int,
                id: Long
            ) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.contentPanel, FavriouteFragmentCompat.newInstance(position))
                    .commit()
                preference.edit().putInt("KEY_POSITION_INT", position).commit()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.spinner.setSelection(position)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
             onBackPressedDispatcher.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}
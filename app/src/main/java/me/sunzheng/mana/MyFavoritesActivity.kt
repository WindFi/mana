package me.sunzheng.mana

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
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
    val binding: ActivityMyFavioursBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_my_faviours)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

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
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}
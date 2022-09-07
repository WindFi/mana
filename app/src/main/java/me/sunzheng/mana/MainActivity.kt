package me.sunzheng.mana

import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SearchView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.core.AnnounceModel
import me.sunzheng.mana.databinding.ActivityMainBinding
import me.sunzheng.mana.home.onair.OnAirFragment
import me.sunzheng.mana.utils.HostUtil
import me.sunzheng.mana.utils.PreferenceManager
import me.sunzheng.mana.utils.RegexUtils
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity @Inject constructor() : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {
    companion object {
        const val CLICK_DELAY_MILLIONSECONDS: Long = 500
    }

    var titles: Array<CharSequence>? = null
    var handler = Handler(Looper.getMainLooper())


    var isJaFirst = false
    val binding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_main)
    }
    private val fragmentPagerAdapter2: FragmentStateAdapter by lazy {
        object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2

            override fun createFragment(position: Int): Fragment =
                OnAirFragment.newInstance(if (position == 0) 2 else 6)
        }
    }

    fun showContentView(visible: Boolean) {
        showCollapsingToolbarLayout(visible)
        if (visible) {
            binding.mainView.mainAnnounceRecyclerview.visibility = View.VISIBLE
        } else {
            binding.mainView.mainAnnounceRecyclerview.visibility = View.GONE
        }
        binding.mainView.mainAnnounceView.visibility =
            binding.mainView.mainAnnounceRecyclerview.visibility
    }

    fun showCollapsingToolbarLayout(active: Boolean) {
        if (binding.mainView.appbarlayout == null) return
        binding.mainView.appbarlayout.setExpanded(active, true)
    }

    fun setData(datas: List<AnnounceModel>) {
        if (binding.mainView.mainAnnounceRecyclerview != null) binding.mainView.mainAnnounceRecyclerview.adapter =
            AnnouceAdapter(datas)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.mainView.toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(true)
        supportActionBar!!.setTitle(R.string.app_name)
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        val toggle: ActionBarDrawerToggle = object : ActionBarDrawerToggle(
            this,
            drawer,
            binding.mainView.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ) {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
//              see https://stackoverflow.com/questions/27117243/disable-hamburger-to-back-arrow-animation-on-toolbar
                super.onDrawerSlide(drawerView, 0f)
            }
        }
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        val navigationView = findViewById<View>(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)
        val account =
            navigationView.getHeaderView(0).findViewById<View>(R.id.nav_title) as AppCompatTextView
        account.text =
            getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, MODE_PRIVATE).getString(
                PreferenceManager.Global.STR_USERNAME, ""
            )
        isJaFirst =
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                getString(
                    PreferenceManager.Global.RES_JA_FIRST_BOOL
                ), false
            )
        titles = arrayOf(
            getText(R.string.title_anim_catalog_tablayout),
            getText(R.string.title_dram_catalog_tablayout)
        )
        binding.mainView.mainViewpager.adapter = fragmentPagerAdapter2
        TabLayoutMediator(
            binding.mainView.mainTablayout,
            binding.mainView.mainViewpager
        ) { tab, position ->
            tab.text = titles?.get(position) ?: ""
        }.attach()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
//        see https://stackoverflow.com/questions/27378981/how-to-use-searchview-in-toolbar-android
        menuInflater.inflate(R.menu.main, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        if (searchItem != null) {
            val searchView = searchItem.actionView as SearchView
            if (searchView != null) {
                searchView.maxWidth = Int.MAX_VALUE
                searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String): Boolean {
                        MenuItemCompat.collapseActionView(searchItem)
                        return false
                    }

                    override fun onQueryTextChange(newText: String): Boolean {
                        return false
                    }
                })
            }
            searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
                override fun onSuggestionSelect(position: Int): Boolean {
                    return false
                }

                override fun onSuggestionClick(position: Int): Boolean {
                    val c = searchView.suggestionsAdapter.cursor
                    if (c == null || !c.moveToPosition(position)) return false
                    val index = c.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)
                    if (index == -1) return false
                    searchView.setQuery(c.getString(index), false)
                    return false
                }
            })
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_search) {
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        val intent = Intent()
        if (id == R.id.nav_settings) {
            intent.component = ComponentName(this, SettingsActivity::class.java)
            handler.postDelayed(
                { if (intent.component != null) startActivity(intent) },
                CLICK_DELAY_MILLIONSECONDS
            )
        } else if (id == R.id.nav_history) {
            intent.component = ComponentName(this, MyFavoritesActivity::class.java)
            handler.postDelayed(
                { if (intent.component != null) startActivity(intent) },
                CLICK_DELAY_MILLIONSECONDS
            )
        } else if (id == R.id.nav_exit) {
            val sharedPreferences =
                getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, MODE_PRIVATE)
            sharedPreferences.edit().putBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, false)
                .commit()
            intent.component = ComponentName(this, LaunchActivity::class.java)
            handler.postDelayed({
                if (intent.component != null) startActivity(intent)
                finish()
            }, CLICK_DELAY_MILLIONSECONDS)
        }
        return false
    }

    /**
     * Created by Sun on 2018/3/12.
     */
    inner class AnnouceAdapter(var values: List<AnnounceModel>?) :
        RecyclerView.Adapter<AnnouceAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_announce_recyclerview, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values!![position]
            val requestManager = Glide.with(holder.itemView.context)
            var request: RequestBuilder<*>? = null
            val host = holder.itemView.context.getSharedPreferences(
                PreferenceManager.Global.STR_SP_NAME,
                MODE_PRIVATE
            )
                .getString(PreferenceManager.Global.STR_KEY_HOST, "")
            if (item.position == 1) {
                holder.itemView.setOnClickListener {
                    val typedValue = TypedValue()
                    val a = obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorPrimary))
                    val color = a.getColor(0, 0)
                    a.recycle()
                    //                        see: https://stackoverflow.com/questions/27611173/how-to-get-accent-color-programmatically/28777489?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                    val builder = CustomTabsIntent.Builder()
                    builder.setToolbarColor(color)
                    builder.setShowTitle(true)
                    val intent = builder.build()
                    intent.launchUrl(this@MainActivity, Uri.parse(item.content))
                }
                if (!TextUtils.isEmpty(HostUtil.makeUp(host, item.image_url))) {
                    request = requestManager.load(item.image_url)
                } else {
                    Log.e("remote error", "id:item.getId()" + "\timage_url is null")
                }
            } else if (item.position == 2) {
                holder.mTextView
                holder.itemView.setOnClickListener {
//                    BangumiDetailsActivity.newInstance(
//                        this@MainActivity,
//                        item.bangumi.id.toString(),
//                        item.bangumi.image,
//                        if (isJaFirst) item.bangumi.name else item.bangumi.nameCn,
//                        holder.mImageView
//                    )
                }
                if (item.bangumi.coverImage != null && !TextUtils.isEmpty(item.bangumi.coverImage.dominantColor)
                    && item.bangumi.coverImage.dominantColor.matches(RegexUtils.ColorPattern.toRegex())
                ) {
                    request =
                        requestManager.load(HostUtil.makeUp(host, item.bangumi.coverImage.url))
                    val options =
                        RequestOptions().placeholder(ColorDrawable(Color.parseColor(item.bangumi.coverImage.dominantColor)))
                    request.apply(options)
                } else {
                }
            }
            request?.into(holder.mImageView)
            if (item.bangumi != null) {
                holder.mTextView.text = if (isJaFirst) item.bangumi.name else item.bangumi.nameCn
            }
            holder.mTextView.visibility = if (item.bangumi == null) View.INVISIBLE else View.VISIBLE
        }

        override fun getItemCount(): Int {
            return if (values == null) 0 else values!!.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var mImageView: ImageView
            var mTextView: TextView

            init {
                mImageView = itemView.findViewById(R.id.item_album)
                mTextView = itemView.findViewById(R.id.item_title_textview)
            }
        }
    }
}
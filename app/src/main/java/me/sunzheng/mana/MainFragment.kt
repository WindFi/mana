package me.sunzheng.mana

import android.app.SearchManager
import android.content.Context
import android.content.Context.SEARCH_SERVICE
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.account.AccountViewModel
import me.sunzheng.mana.core.AnnounceModel
import me.sunzheng.mana.databinding.FragmentMainBinding
import me.sunzheng.mana.home.onair.OnAirFragment
import me.sunzheng.mana.utils.PreferenceManager

@AndroidEntryPoint
class MainFragment : Fragment(),
    NavigationView.OnNavigationItemSelectedListener {
    companion object {
        const val CLICK_DELAY_MILLIONSECONDS: Long = 500
    }

    var titles: Array<CharSequence>? = null
    var handler = Handler(Looper.getMainLooper())


    var isJaFirst = false
    lateinit var binding: FragmentMainBinding

    val viewModel by activityViewModels<AccountViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMainBinding.inflate(inflater)
        return binding.root
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
//        if (binding.mainView.mainAnnounceRecyclerview != null) binding.mainView.mainAnnounceRecyclerview.adapter =
//            MainActivity.AnnouceAdapter(datas)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val account =
            binding.navView.getHeaderView(0).findViewById<View>(R.id.nav_title) as AppCompatTextView
        account.text =
            requireActivity().getSharedPreferences(
                PreferenceManager.Global.STR_SP_NAME,
                AppCompatActivity.MODE_PRIVATE
            ).getString(
                PreferenceManager.Global.STR_USERNAME, ""
            )
        isJaFirst =
            androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getBoolean(
                    getString(
                        PreferenceManager.Global.RES_JA_FIRST_BOOL
                    ), false
                )
        titles = arrayOf(
            getText(R.string.title_anim_catalog_tablayout),
            getText(R.string.title_dram_catalog_tablayout)
        )
        binding.mainView.mainViewpager.adapter =
            object : FragmentStateAdapter(requireParentFragment()) {
                override fun getItemCount(): Int = 2

                override fun createFragment(position: Int): Fragment =
                    OnAirFragment.newInstance(if (position == 0) 2 else 6)
            }
        TabLayoutMediator(
            binding.mainView.mainTablayout,
            binding.mainView.mainViewpager
        ) { tab, position ->
            tab.text = titles?.get(position) ?: ""
        }.attach()

        var appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.fragment_main,
                R.id.nav_favrioute,
                R.id.nav_settings,
                R.id.nav_exit
            ), binding.drawerLayout
        )
        binding.mainView.toolbar.addMenuProvider(optionMenu)
        NavigationUI.setupWithNavController(
            binding.mainView.toolbar,
            findNavController(),
            appBarConfiguration
        )
        binding.navView.setNavigationItemSelectedListener(this)
    }


    private val optionMenu: MenuProvider by lazy {
        object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main, menu)
                val searchItem = menu.findItem(R.id.action_search)
                val searchManager =
                    requireContext().getSystemService(SEARCH_SERVICE) as SearchManager
                if (searchItem != null) {
                    val searchView = searchItem.actionView as SearchView
                    if (searchView != null) {
                        searchView.maxWidth = Int.MAX_VALUE
                        // TODO: test it maybe it is MainActivity
                        searchView.setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
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
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle action bar item clicks here. The action bar will
                // automatically handle clicks on the Home/Up button, so long
                // as you specify a parent activity in AndroidManifest.xml.
                return when (menuItem.itemId) {
                    R.id.action_search -> true
                    else -> false
                }
            }
        }
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return when (item.itemId) {
            R.id.nav_settings -> {
//                findNavController().navigate(MainFragmentDirections.mainToSettings())
                Intent(requireActivity(), MySettingsActivity::class.java).run {
                    startActivity(this)
                }
                true
            }

            R.id.nav_favrioute -> {
                findNavController().navigate(MainFragmentDirections.mainToFavrioute())
                true
            }

            R.id.nav_exit -> {
                val sharedPreferences =
                    requireActivity().getSharedPreferences(
                        PreferenceManager.Global.STR_SP_NAME,
                        Context.MODE_PRIVATE
                    )
                sharedPreferences.edit()
                    .putBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, false)
                    .commit()
                viewModel.checkIsLogin(requireContext())
                true
            }

            else -> false
        }
    }

    /**
     * Created by Sun on 2018/3/12.
     */
//    inner class AnnouceAdapter(var values: List<AnnounceModel>?) :
//        RecyclerView.Adapter<AnnouceAdapter.ViewHolder>() {
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//            val view = LayoutInflater.from(parent.context)
//                .inflate(R.layout.item_announce_recyclerview, parent, false)
//            return ViewHolder(view)
//        }
//
//        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//            val item = values!![position]
//            val requestManager = Glide.with(holder.itemView.context)
//            var request: RequestBuilder<*>? = null
//            val host = holder.itemView.context.getSharedPreferences(
//                PreferenceManager.Global.STR_SP_NAME,
//                MODE_PRIVATE
//            )
//                .getString(PreferenceManager.Global.STR_KEY_HOST, "")
//            if (item.position == 1) {
//                holder.itemView.setOnClickListener {
//                    val typedValue = TypedValue()
//                    val a =
//                        obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorPrimary))
//                    val color = a.getColor(0, 0)
//                    a.recycle()
//                    //                        see: https://stackoverflow.com/questions/27611173/how-to-get-accent-color-programmatically/28777489?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
//                    val builder = CustomTabsIntent.Builder()
//                    builder.setToolbarColor(color)
//                    builder.setShowTitle(true)
//                    val intent = builder.build()
//                    intent.launchUrl(this@MainFragment, Uri.parse(item.content))
//                }
//                if (!TextUtils.isEmpty(HostUtil.makeUp(host, item.image_url))) {
//                    request = requestManager.load(item.image_url)
//                } else {
//                    Log.e("remote error", "id:item.getId()" + "\timage_url is null")
//                }
//            } else if (item.position == 2) {
//                holder.mTextView
//                holder.itemView.setOnClickListener {
////                    BangumiDetailsActivity.newInstance(
////                        this@MainActivity,
////                        item.bangumi.id.toString(),
////                        item.bangumi.image,
////                        if (isJaFirst) item.bangumi.name else item.bangumi.nameCn,
////                        holder.mImageView
////                    )
//                }
//                if (item.bangumi.coverImage != null && !TextUtils.isEmpty(item.bangumi.coverImage.dominantColor)
//                    && item.bangumi.coverImage.dominantColor.matches(RegexUtils.ColorPattern.toRegex())
//                ) {
//                    request =
//                        requestManager.load(HostUtil.makeUp(host, item.bangumi.coverImage.url))
//                    val options =
//                        RequestOptions().placeholder(ColorDrawable(Color.parseColor(item.bangumi.coverImage.dominantColor)))
//                    request.apply(options)
//                } else {
//                }
//            }
//            request?.into(holder.mImageView)
//            if (item.bangumi != null) {
//                holder.mTextView.text =
//                    if (isJaFirst) item.bangumi.name else item.bangumi.nameCn
//            }
//            holder.mTextView.visibility =
//                if (item.bangumi == null) View.INVISIBLE else View.VISIBLE
//        }
//
//        override fun getItemCount(): Int {
//            return if (values == null) 0 else values!!.size
//        }
//
//        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//            var mImageView: ImageView
//            var mTextView: TextView
//
//            init {
//                mImageView = itemView.findViewById(R.id.item_album)
//                mTextView = itemView.findViewById(R.id.item_title_textview)
//            }
//        }
//    }
}
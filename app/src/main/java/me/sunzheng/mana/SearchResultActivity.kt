package me.sunzheng.mana

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import me.sunzheng.mana.core.net.Status
import me.sunzheng.mana.core.net.v2.database.BangumiEntity
import me.sunzheng.mana.core.net.v2.showToast
import me.sunzheng.mana.databinding.ActivitySearchResultBinding
import me.sunzheng.mana.home.search.HistorySuggestionProvider
import me.sunzheng.mana.home.search.SearchResultAdapter
import me.sunzheng.mana.home.search.SearchViewModel

@AndroidEntryPoint
class SearchResultActivity : AppCompatActivity() {
    var isLoading = false
    var loadMoreable = true

    val viewModel by viewModels<SearchViewModel>()
    val binding: ActivitySearchResultBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.activity_search_result)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                if (!isLoading && layoutManager!!.findFirstCompletelyVisibleItemPosition() + layoutManager.childCount >= layoutManager.itemCount && loadMoreable) {
                    viewModel.loadMore().observe(this@SearchResultActivity) {
                        when (it.code) {
                            Status.SUCCESS -> {
                                loadMoreData(it.data)
                            }

                            Status.ERROR -> {
                                it.message?.run {
                                    showToast(this)
                                }
                            }

                            Status.LOADING -> {
                                loadMoreData(it.data)
                            }
                        }
                    }
                }
            }
        })

        binding.searchSwiperefreshlayout.isEnabled = false
        handleIntent(intent)
    }

    private fun loadMoreData(item: Collection<BangumiEntity>?) {
        if (item?.isNullOrEmpty() == true) {
            loadMoreable = false
        } else {
            var adapter =
                binding.recyclerView.adapter as SearchResultAdapter
            var position = adapter.mValues.size()
            var count = item.size
            adapter.mValues.addAll(item)
            adapter.notifyItemRangeInserted(position, count)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (Intent.ACTION_SEARCH == intent.action) {
            val key = intent.getStringExtra(SearchManager.QUERY)
            key?.run {
                val searchRecentSuggestions = SearchRecentSuggestions(
                    this@SearchResultActivity,
                    HistorySuggestionProvider.AUTHORITY,
                    HistorySuggestionProvider.MODE
                )
                searchRecentSuggestions.saveRecentQuery(key, null)
                query(key)
            }
        }
    }

    private fun query(key: String) {
        showProgressIntractor(true)
        loadMoreable = true
        viewModel.query(key).observe(this) { response ->
            when (response.code) {
                Status.SUCCESS -> {
                    showProgressIntractor(false)
                    if (response.data?.isEmpty() == true) {
                        showEmptyView(true)
                    } else {
                        response.data?.takeIf { it.isNotEmpty() }?.run {
                            showEmptyView(false)
                            var adapter = SearchResultAdapter(this) { v, _, _, m ->
                                if (m is BangumiEntity) {
                                    BangumiDetailsActivity.newInstance(
                                        this@SearchResultActivity,
                                        m,
                                        v.findViewById(R.id.item_album)
                                    )
                                }
                            }
                            if (binding.recyclerView.adapter == null) binding.recyclerView.adapter =
                                adapter else binding.recyclerView.swapAdapter(adapter, false)
                        }

                    }
                }

                Status.ERROR -> {
                    showProgressIntractor(false)
                    response.message?.run {
                        showToast(this)
                    }
                }

                Status.LOADING -> {
                    response.data?.run {
                        var adapter = SearchResultAdapter(this) { v, _, _, m ->
                            if (m is BangumiEntity) {
                                BangumiDetailsActivity.newInstance(
                                    this@SearchResultActivity,
                                    m,
                                    v.findViewById(R.id.item_album)
                                )
                            }
                        }
                        if (binding.recyclerView.adapter == null) binding.recyclerView.adapter =
                            adapter else binding.recyclerView.swapAdapter(adapter, false)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        val searchItem = menu.findItem(R.id.action_search)
        val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
        if (searchItem != null) {
            var mSearchView = searchItem.actionView as SearchView
            if (mSearchView != null) {
                mSearchView.maxWidth = Int.MAX_VALUE
                mSearchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
                mSearchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String): Boolean {
//                        MenuItemCompat.collapseActionView(searchItem)
                        searchItem.collapseActionView()
                        mSearchView.setQuery(query, false)
                        return false
                    }

                    override fun onQueryTextChange(newText: String): Boolean {
                        return false
                    }
                })
                mSearchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
                    override fun onSuggestionSelect(position: Int): Boolean {
                        return false
                    }

                    override fun onSuggestionClick(position: Int): Boolean {
                        val c = mSearchView.suggestionsAdapter.cursor
                        if (c == null || !c.moveToPosition(position)) return false
                        val index = c.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)
                        if (index == -1) return false
                        mSearchView.setQuery(c.getString(index), false)
                        return false
                    }
                })
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            onBackPressedDispatcher.onBackPressed()
            true
        }

        else -> false
    }

    fun showProgressIntractor(active: Boolean) {
        binding.searchSwiperefreshlayout.isRefreshing = active
    }


    private fun showEmptyView(active: Boolean) {
        binding.emptyContentTextview.isVisible = active
        binding.recyclerView.isVisible = !active
    }
}
package me.sunzheng.mana;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.search.HistorySuggestionProvider;
import me.sunzheng.mana.home.search.SearchPresenterImpl;
import me.sunzheng.mana.utils.App;

public class SearchResultActivity extends AppCompatActivity implements HomeContract.Search.View {

    RecyclerView mRecyclerView;
    HomeContract.Search.Presenter mPresenter;
    ContentLoadingProgressBar progressBar;
    SwipeRefreshLayout mSwipeRefreshLayout;
    boolean isLoading = false;
    View emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        progressBar = (ContentLoadingProgressBar) findViewById(R.id.progressbar);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (!isLoading && layoutManager.findFirstCompletelyVisibleItemPosition() + layoutManager.getChildCount() >= layoutManager.getItemCount()) {
                    mPresenter.loadMore();
                }
            }
        });
        emptyView = findViewById(R.id.empty_content_textview);
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.search_swiperefreshlayout);
//        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
//            @Override
//            public void onRefresh() {
//                mPresenter.query();
//            }
//        });
//        mSwipeRefreshLayout.setProgressViewOffset(true, 1900, 200);
//        mSwipeRefreshLayout.setOnChildScrollUpCallback(new SwipeRefreshLayout.OnChildScrollUpCallback() {
//            @Override
//            public boolean canChildScrollUp(SwipeRefreshLayout parent, @Nullable View child) {
//                return true;
//            }
//        });
        mPresenter = new SearchPresenterImpl(this, ((App) getApplicationContext()).getRetrofit().create(HomeApiService.Bangumi.class));
        setPresenter(mPresenter);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String key = intent.getStringExtra(SearchManager.QUERY);
            SearchRecentSuggestions searchRecentSuggestions = new SearchRecentSuggestions(this, HistorySuggestionProvider.AUTHORITY, HistorySuggestionProvider.MODE);
            searchRecentSuggestions.saveRecentQuery(key, null);
            query(key);
        }
    }

    private void query(String key) {
        mPresenter.query(key);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
        }
        if (searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        } else {

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setPresenter(HomeContract.Search.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public void empty() {
        showEmptyView(true);
        AppCompatTextView messageTextView = (AppCompatTextView) findViewById(R.id.empty_content_textview);
        if (messageTextView != null)
            messageTextView.setText("No result");
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        showEmptyView(false);
        if (mRecyclerView.getLayoutManager() == null) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        }
        if (mRecyclerView.getAdapter() == null)
            mRecyclerView.setAdapter(adapter);
        else
            mRecyclerView.swapAdapter(adapter, false);
    }

    @Override
    public void notifyDataSetChanged() {
        mRecyclerView.getAdapter().notifyDataSetChanged();
        isLoading = false;
    }

    @Override
    public void showProgressIntractor(boolean active) {
        mSwipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void showLoadMoreProgressIntractor(boolean active) {
        if (mSwipeRefreshLayout == null)
            return;
        isLoading = active;
        mSwipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public Context getContext() {
        return this;
    }

    private void showEmptyView(boolean active) {
        emptyView.setVisibility(active ? View.VISIBLE : View.GONE);
        mRecyclerView.setVisibility(active ? View.GONE : View.VISIBLE);
    }
}

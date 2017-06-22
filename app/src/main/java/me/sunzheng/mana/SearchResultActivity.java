package me.sunzheng.mana;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.search.SearchPresenterImpl;
import me.sunzheng.mana.utils.App;

public class SearchResultActivity extends AppCompatActivity implements HomeContract.Search.View {

    RecyclerView mRecyclerView;
    HomeContract.Search.Presenter mPresenter;
    ContentLoadingProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        progressBar = (ContentLoadingProgressBar) findViewById(R.id.progressbar);

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
            mPresenter.query(key);
        }
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
    public void empty(String message) {

    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (mRecyclerView.getLayoutManager() == null) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        }
        if (mRecyclerView.getAdapter() == null)
            mRecyclerView.setAdapter(adapter);
        else
            mRecyclerView.swapAdapter(adapter, true);
    }

    @Override
    public void notifyDataSetChanged() {

    }

    @Override
    public void showProgressIntractor(boolean active) {
        if (active)
            progressBar.show();
        else
            progressBar.hide();
    }
}

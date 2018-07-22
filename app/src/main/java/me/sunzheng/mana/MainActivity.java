package me.sunzheng.mana;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.DrawableTypeRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import java.util.List;

import me.sunzheng.mana.core.AnnounceModel;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.main.AnnoucePresenterImpl;
import me.sunzheng.mana.home.onair.OnAirFragment;
import me.sunzheng.mana.home.onair.OnAirPresenterImpl;
import me.sunzheng.mana.home.onair.respository.DataRepositoryImpl;
import me.sunzheng.mana.utils.App;
import me.sunzheng.mana.utils.PreferenceManager;
import me.sunzheng.mana.utils.RegexUtils;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, HomeContract.Annouce.View {
    final static long CLICK_DELAY_MILLIONSECONDS = 500;
    TabLayout tabLayout;
    ViewPager mViewPager;
    View mAnnounceView;
    HomeApiService.OnAir apiService;
    CharSequence[] titles;
    Handler handler = new Handler();
    boolean isJaFirst = false;
    FragmentStatePagerAdapter fragmentPagerAdapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
        @Override
        public void finishUpdate(ViewGroup container) {
            super.finishUpdate(container);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            OnAirFragment fragment = (OnAirFragment) super.instantiateItem(container, position);
            HomeContract.OnAir.Presenter presenter = new OnAirPresenterImpl(fragment, new DataRepositoryImpl(MainActivity.this, apiService));
            fragment.setPresenter(presenter);
            presenter.load(fragment.getArguments().getInt(OnAirFragment.INT_ARGS_TYPE));
            return fragment;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            OnAirFragment fragment = OnAirFragment.newInstance(position % 2 == 0 ? OnAirFragment.INT_TYPE_ANIMATION : OnAirFragment.INT_TYPE_DRAMA);
            return fragment;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titles[position];
        }
    };
    HomeContract.Annouce.Presenter mPresenter;
    RecyclerView mRecyclerView;
    AppBarLayout appBarLayout;

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void setPresenter(HomeContract.Annouce.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showProgressIntractor(boolean active) {

    }

    @Override
    public void showContentView(boolean visible) {
        showCollapsingToolbarLayout(visible);
        if (visible) {
            mRecyclerView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.GONE);
        }
        mAnnounceView.setVisibility(mRecyclerView.getVisibility());
    }

    void showCollapsingToolbarLayout(boolean active) {
        if (appBarLayout == null)
            return;
        appBarLayout.setExpanded(active, true);
    }

    @Override
    public void setData(List<AnnounceModel> datas) {
        if (mRecyclerView != null)
            mRecyclerView.setAdapter(new AnnouceAdapter(datas));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle(R.string.app_name);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
//              see https://stackoverflow.com/questions/27117243/disable-hamburger-to-back-arrow-animation-on-toolbar
                super.onDrawerSlide(drawerView, 0);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        AppCompatTextView account = (AppCompatTextView) navigationView.getHeaderView(0).findViewById(R.id.nav_title);
        account.setText(getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE).getString(PreferenceManager.Global.STR_USERNAME, ""));
        isJaFirst = android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(PreferenceManager.Global.RES_JA_FIRST_BOOL), false);
        apiService = ((App) getApplicationContext()).getRetrofit().create(HomeApiService.OnAir.class);
        titles = new CharSequence[]{getText(R.string.title_anim_catalog_tablayout), getText(R.string.title_dram_catalog_tablayout)};
        tabLayout = (TabLayout) findViewById(R.id.main_tablayout);
        mViewPager = (ViewPager) findViewById(R.id.main_viewpager);
        mViewPager.setAdapter(fragmentPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setupWithViewPager(mViewPager, true);
        mRecyclerView = (RecyclerView) findViewById(R.id.main_announce_recyclerview);
        appBarLayout = (AppBarLayout) findViewById(R.id.main_appbarlayout);
        mAnnounceView = findViewById(R.id.main_announce_view);
        setPresenter(new AnnoucePresenterImpl(this, ((App) getApplicationContext()).getRetrofit().create(HomeApiService.Announce.class)));
        mPresenter.load();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mRecyclerView != null && mRecyclerView.getLayoutManager() != null && mRecyclerView.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) mRecyclerView.getLayoutManager();
            layoutManager.setSpanCount(Integer.valueOf(getResources().getString(R.string.main_announce_span_count)));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        see https://stackoverflow.com/questions/27378981/how-to-use-searchview-in-toolbar-android
        getMenuInflater().inflate(R.menu.main, menu);
        final MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchItem != null) {
            final SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                searchView.setMaxWidth(Integer.MAX_VALUE);
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        MenuItemCompat.collapseActionView(searchItem);

                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        return false;
                    }
                });
            }
            searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
                @Override
                public boolean onSuggestionSelect(int position) {
                    return false;
                }

                @Override
                public boolean onSuggestionClick(int position) {
                    Cursor c = searchView.getSuggestionsAdapter().getCursor();
                    if (c == null || !c.moveToPosition(position))
                        return false;
                    int index = c.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1);
                    if (index == -1)
                        return false;
                    searchView.setQuery(c.getString(index), false);
                    return false;
                }
            });
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        final Intent intent = new Intent();

        if (id == R.id.nav_settings) {
            intent.setComponent(new ComponentName(this, SettingsActivity.class));
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (intent.getComponent() != null)
                        startActivity(intent);
                }
            }, CLICK_DELAY_MILLIONSECONDS);
        } else if (id == R.id.nav_history) {
            intent.setComponent(new ComponentName(this, MyFavoritesActivity.class));
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (intent.getComponent() != null)
                        startActivity(intent);
                }
            }, CLICK_DELAY_MILLIONSECONDS);
        } else if (id == R.id.nav_exit) {
            SharedPreferences sharedPreferences = getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE);
            sharedPreferences.edit().putBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, false).commit();
            intent.setComponent(new ComponentName(this, LaunchActivity.class));
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (intent.getComponent() != null)
                        startActivity(intent);
                    finish();
                }
            }, CLICK_DELAY_MILLIONSECONDS);
        }
        return false;
    }

    /**
     * Created by Sun on 2018/3/12.
     */

    public class AnnouceAdapter extends RecyclerView.Adapter<AnnouceAdapter.ViewHolder> {
        List<AnnounceModel> values;

        public AnnouceAdapter(List<AnnounceModel> values) {
            this.values = values;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announce_recyclerview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final AnnounceModel item = values.get(position);
            RequestManager requestManager = Glide.with(holder.itemView.getContext());
            DrawableTypeRequest request = null;
            if (item.getPosition() == 1) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TypedValue typedValue = new TypedValue();
                        TypedArray a = obtainStyledAttributes(typedValue.data, new int[]{R.attr.colorPrimary});
                        int color = a.getColor(0, 0);
                        a.recycle();
//                        see: https://stackoverflow.com/questions/27611173/how-to-get-accent-color-programmatically/28777489?utm_medium=organic&utm_source=google_rich_qa&utm_campaign=google_rich_qa
                        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                        builder.setToolbarColor(color);
                        builder.setShowTitle(true);
                        CustomTabsIntent intent = builder.build();
                        intent.launchUrl(MainActivity.this, Uri.parse(item.getContent()));
                    }
                });
                if (!TextUtils.isEmpty(item.getImage_url())) {
                    request = requestManager.load(item.getImage_url());
                } else {
                    Log.e("remote error", "id:item.getId()" + "\timage_url is null");
                }
            } else if (item.getPosition() == 2) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BangumiDetailsActivity.newInstance(MainActivity.this, item.getBangumi().getId().toString(), item.getBangumi().getImage(), isJaFirst ? item.getBangumi().getName() : item.getBangumi().getNameCn(),
                                holder.mImageView);
                    }
                });
                if (item.getBangumi().getCoverImage() != null && !TextUtils.isEmpty(item.getBangumi().getCoverImage().dominantColor)
                        && item.getBangumi().getCoverImage().dominantColor.matches(RegexUtils.ColorPattern)) {
                    request = requestManager.load(item.getBangumi().getCoverImage().url);
                    request.placeholder(new ColorDrawable(Color.parseColor(item.getBangumi().getCoverImage().dominantColor)));
                } else {

                }
            }
            if (request != null)
                request.into(holder.mImageView);
            if (item.getBangumi() != null) {
                holder.mTextView.setText(isJaFirst ? item.getBangumi().getName() : item.getBangumi().getNameCn());
            }
            holder.mTextView.setVisibility(item.getBangumi() == null ? View.INVISIBLE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return values == null ? 0 : values.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            public ImageView mImageView;
            public TextView mTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                mImageView = itemView.findViewById(R.id.item_album);
                mTextView = itemView.findViewById(R.id.item_title_textview);
            }
        }
    }
}

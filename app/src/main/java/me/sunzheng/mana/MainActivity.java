package me.sunzheng.mana;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SearchViewCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.onair.OnAirFragment;
import me.sunzheng.mana.home.onair.OnAirPresenterImpl;
import me.sunzheng.mana.utils.App;
import me.sunzheng.mana.utils.PreferenceManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    final static long CLICK_DELAY_MILLIONSECONDS = 500;
    TabLayout tabLayout;
    ViewPager mViewPager;
    HomeApiService.OnAir apiService;
    CharSequence[] titles;
    Handler handler = new Handler();
    FragmentStatePagerAdapter fragmentPagerAdapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            OnAirFragment fragment = (OnAirFragment) super.instantiateItem(container, position);
            HomeContract.OnAir.Presenter presenter = new OnAirPresenterImpl(fragment, apiService);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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
        apiService = ((App) getApplicationContext()).getRetrofit().create(HomeApiService.OnAir.class);
        titles = new CharSequence[]{getText(R.string.title_anim_catalog_tablayout), getText(R.string.title_dram_catalog_tablayout)};
        tabLayout = (TabLayout) findViewById(R.id.main_tablayout);
        mViewPager = (ViewPager) findViewById(R.id.main_viewpager);
        mViewPager.setAdapter(fragmentPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setupWithViewPager(mViewPager, true);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        see https://stackoverflow.com/questions/27378981/how-to-use-searchview-in-toolbar-android
        getMenuInflater().inflate(R.menu.main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = null;
        SearchViewCompat.setOnQueryTextListener(searchView, new SearchViewCompat.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
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
        } else if (id == R.id.nav_history) {
            intent.setComponent(new ComponentName(this, MyFavoritesActivity.class));
        } else if (id == R.id.nav_exit) {
            SharedPreferences sharedPreferences = getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE);
            sharedPreferences.edit().putBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, false).commit();
            intent.setComponent(new ComponentName(this, LoginActivity.class));
        }
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (intent.getComponent() != null)
                    startActivity(intent);
            }
        }, CLICK_DELAY_MILLIONSECONDS);
        return false;
    }
}

package me.sunzheng.mana;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.onair.OnAirFragment;
import me.sunzheng.mana.home.onair.OnAirPresenterImpl;
import me.sunzheng.mana.utils.App;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    TabLayout tabLayout;
    ViewPager mViewPager;
    HomeApiService.OnAir apiService;
    CharSequence[] titles;
    FragmentStatePagerAdapter fragmentPagerAdapter = new FragmentStatePagerAdapter(getSupportFragmentManager()) {

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            OnAirFragment fragment = OnAirFragment.newInstance(position % 2 == 0 ? OnAirFragment.INT_TYPE_ANIMATION : OnAirFragment.INT_TYPE_DRAMA);
            fragment.setPresenter(new OnAirPresenterImpl(fragment, apiService));
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
//        init layouts
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

        if (id == R.id.nav_settings) {
            // TODO: 2017/5/22 app settings
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_history) {
            // TODO: 2017/5/22 history
            Intent intent = new Intent(this, MyFavouritesActivity.class);
            startActivity(intent);
        } else if (id == R.id.account) {
            // TODO: 2017/5/22 account change to account page
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

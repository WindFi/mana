package me.sunzheng.mana;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.mybangumi.FavoriteFragment;
import me.sunzheng.mana.home.mybangumi.MyFavouritePresenter;
import me.sunzheng.mana.utils.App;

/**
 * MyFaviours activity
 * for list the faviours
 * and click any item goto {@link BangumiDetailsActivity}
 */
public class MyFavouritesActivity extends AppCompatActivity {
    HomeApiService.MyBangumi apiService;
    FavoriteFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_faviours);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fragment = FavoriteFragment.newInstance();
        fragment.setPresenter(new MyFavouritePresenter(fragment, ((App) getApplicationContext()).getRetrofit().create(HomeApiService.MyBangumi.class)));
        getSupportFragmentManager().beginTransaction().replace(R.id.contentPanel, fragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}

package me.sunzheng.mana;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.mybangumi.FavoritesFragment;
import me.sunzheng.mana.home.mybangumi.MyFavoritesPresenter;
import me.sunzheng.mana.home.mybangumi.respository.DataRespositoryImpl;
import me.sunzheng.mana.utils.App;

/**
 * MyFaviours activity
 * for list the faviours
 * and click any item goto {@link BangumiDetailsActivity}
 */
public class MyFavoritesActivity extends AppCompatActivity {
    FavoritesFragment fragment;
    AppCompatSpinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_faviours);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        spinner = findViewById(R.id.spinner);
        fragment = FavoritesFragment.newInstance();
        fragment.setPresenter(new MyFavoritesPresenter(fragment, new DataRespositoryImpl(MyFavoritesActivity.this, ((App) getApplicationContext()).getRetrofit().create(HomeApiService.MyBangumi.class))));
        getSupportFragmentManager().beginTransaction().replace(R.id.contentPanel, fragment).commit();
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fragment.onFilter(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}

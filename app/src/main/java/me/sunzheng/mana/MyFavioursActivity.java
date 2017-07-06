package me.sunzheng.mana;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import me.sunzheng.mana.home.mybangumi.FavioursFragment;

/**
 * MyFaviours activity
 * for list the faviours
 * and click any item goto {@link BangumiDetailsActivity}
 */
public class MyFavioursActivity extends AppCompatActivity {
    FavioursFragment fragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_faviours);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fragment = FavioursFragment.newInstance();
        // TODO: 2017/7/4  setPresenter
        fragment.setPresenter(null);
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

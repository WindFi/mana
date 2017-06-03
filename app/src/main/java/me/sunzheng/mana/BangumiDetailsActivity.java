package me.sunzheng.mana;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.bangumi.BangumiDetailsFragment;
import me.sunzheng.mana.home.bangumi.BangumiDetailsPresenterImpl;

/**
 * I wannt passed the image from anothers
 */
public class BangumiDetailsActivity extends AppCompatActivity {
    public static final String ARGS_ID_STR="id";
    public static final String ARGS_ABLUM_URL_STR="imageurl";

    BangumiDetailsFragment fragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode_details);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fragment=BangumiDetailsFragment.newInstance(getIntent().getExtras());
        fragment.setPresenter(new BangumiDetailsPresenterImpl(fragment,((App)getApplicationContext()).getRetrofit().create(HomeApiService.Bangumi.class)));
        // TODO: 2017/5/27  setPresenter
        getSupportFragmentManager().beginTransaction().replace(R.id.contentview, fragment).commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}

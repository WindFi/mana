package me.sunzheng.mana;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.bangumi.BangumiDetailsFragment;
import me.sunzheng.mana.home.bangumi.BangumiDetailsPresenterImpl;

/**
 * I wannt passed the image from anothers
 */
public class BangumiDetailsActivity extends AppCompatActivity {
    public static final String ARGS_ID_STR = "id";
    public static final String ARGS_ABLUM_URL_STR = "imageurl";
    public static final String PAIR_IMAGE_STR="pair_image";
    BangumiDetailsFragment fragment;
    ImageView mBannerImageView;
    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode_details);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mBannerImageView=(ImageView) findViewById(R.id.banner_imageview);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        fragment = BangumiDetailsFragment.newInstance(getIntent().getExtras());
        fragment.setPresenter(new BangumiDetailsPresenterImpl(fragment, ((App) getApplicationContext()).getRetrofit().create(HomeApiService.Bangumi.class)));
        // TODO: 2017/5/27  setPresenter
        getSupportFragmentManager().beginTransaction().replace(R.id.contentview, fragment).commit();
        // TODO: 2017/6/4 default banner 
        Glide.with(this).load(savedInstanceState.getString(ARGS_ABLUM_URL_STR)).into(mBannerImageView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

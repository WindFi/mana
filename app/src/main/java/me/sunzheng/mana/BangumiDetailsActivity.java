package me.sunzheng.mana;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.bangumi.BangumiDetailsFragment;
import me.sunzheng.mana.home.bangumi.BangumiDetailsPresenterImpl;
import me.sunzheng.mana.utils.App;

/**
 * I wannt passed the image from anothers
 */
public class BangumiDetailsActivity extends AppCompatActivity {
    public static final String ARGS_ID_STR = "id";
    public static final String ARGS_ABLUM_URL_STR = "imageurl";
    public static final String ARGS_TITLE_STR = "title";
    public static final String PAIR_IMAGE_STR = "pair_image";
    BangumiDetailsFragment fragment;
    ImageView mBannerImageView;
    CollapsingToolbarLayout mHeaderCollapsingToolbarLayout;
    Toolbar mToolbar;

    public static void newInstance(Activity activity, String id, String imageUrl, String title, View... imageView) {
        Intent intent = new Intent(activity, BangumiDetailsActivity.class);
        Bundle extras = new Bundle();
        extras.putString(BangumiDetailsActivity.ARGS_ABLUM_URL_STR, imageUrl);
        extras.putString(BangumiDetailsActivity.ARGS_ID_STR, id);
        extras.putString(BangumiDetailsActivity.ARGS_TITLE_STR, title);
        intent.putExtras(extras);

        Pair<View, String> pair = Pair.create((View) imageView[0], BangumiDetailsActivity.PAIR_IMAGE_STR);

        ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pair);
        activity.startActivity(intent, optionsCompat.toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_episode_details);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mBannerImageView = (ImageView) findViewById(R.id.banner_imageview);
        mHeaderCollapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.header_collaspingtoolbarlayout);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState == null)
            savedInstanceState = getIntent().getExtras();
        if (savedInstanceState == null)
            finish();
        getSupportActionBar().setTitle(savedInstanceState.getString(ARGS_TITLE_STR));
        fragment = BangumiDetailsFragment.newInstance(savedInstanceState);
        fragment.setPresenter(new BangumiDetailsPresenterImpl(fragment, ((App) getApplicationContext()).getRetrofit().create(HomeApiService.Bangumi.class)));
        // TODO: 2017/5/27  setPresenter
        getSupportFragmentManager().beginTransaction().replace(R.id.contentPanel, fragment).commit();
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

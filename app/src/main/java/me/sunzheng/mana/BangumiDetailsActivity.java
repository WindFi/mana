package me.sunzheng.mana;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.bangumi.BangumiDetailsFragment;
import me.sunzheng.mana.home.bangumi.BangumiDetailsPresenterImpl;
import me.sunzheng.mana.home.bangumi.respository.DataRespositoryImpl;
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

    public static void newInstance(Activity activity, String id, String imageUrl, String title, View... imageView) {
        Intent intent = new Intent(activity, BangumiDetailsActivity.class);
        Bundle extras = new Bundle();
        extras.putString(BangumiDetailsActivity.ARGS_ABLUM_URL_STR, imageUrl);
        extras.putString(BangumiDetailsActivity.ARGS_ID_STR, id);
        extras.putString(BangumiDetailsActivity.ARGS_TITLE_STR, title);
        intent.putExtras(extras);
        Pair<View, String> pair0 = Pair.create((View) imageView[0], BangumiDetailsActivity.PAIR_IMAGE_STR);
        ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, pair0);
        ActivityCompat.startActivity(activity, intent, optionsCompat.toBundle());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bangumi_details);
        if (savedInstanceState == null)
            savedInstanceState = getIntent().getExtras();
        if (savedInstanceState == null)
            finish();
        fragment = BangumiDetailsFragment.newInstance(savedInstanceState);
        fragment.setPresenter(new BangumiDetailsPresenterImpl(fragment, new DataRespositoryImpl(this, ((App) getApplicationContext()).getRetrofit().create(HomeApiService.Bangumi.class), savedInstanceState.getString(ARGS_ID_STR))));
        getSupportFragmentManager().beginTransaction().add(R.id.contentPanel, fragment).commit();
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

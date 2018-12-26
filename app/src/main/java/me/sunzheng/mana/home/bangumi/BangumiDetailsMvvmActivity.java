package me.sunzheng.mana.home.bangumi;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import me.sunzheng.mana.R;
import me.sunzheng.mana.home.bangumi.ui.bangumidetailsmvvm.BangumiDetailsMvvmFragment;

public class BangumiDetailsMvvmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bangumi_details_mvvm_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, BangumiDetailsMvvmFragment.newInstance())
                    .commitNow();
        }
    }
}

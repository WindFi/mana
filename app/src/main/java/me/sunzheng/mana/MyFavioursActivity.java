package me.sunzheng.mana;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * MyFaviours activity
 * for list the faviours
 * and click any item goto {@link BangumiDetailsActivity}
 */
public class MyFavioursActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_faviours);
    }
}

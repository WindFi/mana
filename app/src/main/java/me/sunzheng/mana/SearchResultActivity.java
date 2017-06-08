package me.sunzheng.mana;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SearchResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);
    }

    @Override
    public boolean onSearchRequested() {
        return super.onSearchRequested();
    }
}

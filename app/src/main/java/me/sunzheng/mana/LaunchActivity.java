package me.sunzheng.mana;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;

import me.sunzheng.mana.utils.PreferenceManager;

public class LaunchActivity extends Activity {
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE);
        Intent intent = null;
        if (!TextUtils.isEmpty(sharedPreferences.getString(PreferenceManager.Global.STR_KEY_HOST, "")) && !TextUtils.isEmpty(sharedPreferences.getString(PreferenceManager.Global.STR_USERNAME, ""))) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }
}

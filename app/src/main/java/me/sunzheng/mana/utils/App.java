package me.sunzheng.mana.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Sun on 2017/5/21.
 */

public class App extends MultiDexApplication {
    Retrofit mRetrofit;
    String TAG = getClass().getSimpleName();
    SharedPreferences sharedPreferences;
    boolean isConfigChanged = false;
    SharedPreferences.OnSharedPreferenceChangeListener configListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(PreferenceManager.Global.STR_KEY_HOST)) {
                isConfigChanged = true;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Glide.get(this).setMemoryCategory(MemoryCategory.NORMAL);
        sharedPreferences = getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE);
        sharedPreferences.registerOnSharedPreferenceChangeListener(configListener);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    public Retrofit getRetrofit() {
        if (mRetrofit == null || isConfigChanged)
            try {
                mRetrofit = defaultRetrofit();
                isConfigChanged = false;
            } catch (IllegalArgumentException e) {

            }
        return mRetrofit;
    }

    private String getHost() {
        String host = sharedPreferences.getString(PreferenceManager.Global.STR_KEY_HOST, "");
        Log.i(TAG, host);
        return host;
    }

    private Retrofit defaultRetrofit() throws IllegalArgumentException {
        return new Retrofit.Builder()
                .baseUrl(getHost())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(defaultOkHttpClient())
                .build();

    }

    private OkHttpClient defaultOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10000, TimeUnit.MILLISECONDS)
                .cookieJar(new JavaNetCookieJar(new CookieManager(new PersistentHttpCookieStore(this), CookiePolicy.ACCEPT_ALL)))
                .build();
    }
}

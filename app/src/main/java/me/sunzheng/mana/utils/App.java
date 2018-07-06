package me.sunzheng.mana.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.multidex.MultiDexApplication;
import android.support.v4.os.LocaleListCompat;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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
        initGlobalPreferences();
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
                .addNetworkInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request();
                        Headers headers = request.headers();
                        String s = headers.get("User-Agent");
                        headers = headers.newBuilder().set("User-Agent", s + "; " + getUserAgent()).build();
                        request = request.newBuilder().headers(headers).build();
                        return chain.proceed(request);
                    }
                })
                .cookieJar(new JavaNetCookieJar(new CookieManager(new PersistentHttpCookieStore(this), CookiePolicy.ACCEPT_ALL)))
                .build();
    }

    String getUserAgent() {
        return getApplicationLabel() + "/" + getVersionCode();
    }

    String getVersionCode() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
            return String.valueOf(packageInfo.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "0";
    }

    String getApplicationLabel() {
        return getApplicationInfo().loadLabel(getPackageManager()).toString();
    }

    String getLanguage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return LocaleListCompat.getDefault().get(0).getLanguage();
        }
        return Locale.getDefault().getLanguage();
    }

    void initGlobalPreferences() {
//        ja first
        SharedPreferences __sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        if (!__sharedPreferences.contains(getString(PreferenceManager.Global.RES_JA_FIRST_BOOL))) {
            String displayLanguage = getLanguage();
            boolean isJaLanguage = displayLanguage.toLowerCase().contains("ja");
            __sharedPreferences.edit().putBoolean(getString(PreferenceManager.Global.RES_JA_FIRST_BOOL), isJaLanguage).commit();
        }
    }
}

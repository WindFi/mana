package me.sunzheng.mana.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
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

    @Override
    public void onCreate() {
        super.onCreate();
        Glide.get(this).setMemoryCategory(MemoryCategory.NORMAL);
    }

    public Retrofit getRetrofit() {
        return mRetrofit;
    }

    public void initClient() {
        mRetrofit = defaultRetrofit();
    }
    private String getHost() {
        SharedPreferences sharedPreferences = getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE);
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
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
//                        Request request=chain.request().newBuilder().addHeader("Accept","application/json,text/plain,*/*")
                        Response result = chain.proceed(chain.request());
                        return result;
                    }
                })
                .build();
    }

}

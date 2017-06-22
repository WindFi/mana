package me.sunzheng.mana.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.multidex.MultiDexApplication;

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
    @Override
    public void onCreate() {
        super.onCreate();
        Glide.get(this).setMemoryCategory(MemoryCategory.NORMAL);
    }

    public Retrofit getRetrofit() {
        if (mRetrofit == null)
            mRetrofit = defaultRetrofit();
        return mRetrofit;
    }
    private String getHost(){
        SharedPreferences sharedPreferences=getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(PreferenceManager.Global.STR_KEY_HOST,"https://suki.moe").commit();
        return sharedPreferences.getString(PreferenceManager.Global.STR_KEY_HOST,"https://suki.moe");
    }
    private Retrofit defaultRetrofit() {
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
//                                .addHeader("Accept-Encoding","gzip, deflate, br")
//                                .addHeader("Accept-Language","en-US,zh-CN;q=0.8,zh;q=0.6,ja-JP;q=0.4,ja;q=0.2,en;q=0.2")
//                                .addHeader("User-Agent","Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36").build();
                        Response result=chain.proceed(chain.request());
                        return result;
                    }
                })
                .build();
    }

}

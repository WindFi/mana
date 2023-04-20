package me.sunzheng.mana.core.net.v2.di

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.text.TextUtils
import androidx.multidex.MultiDexApplication
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.sunzheng.mana.core.net.LiveDataCallAdapterFactory
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.database.*
import me.sunzheng.mana.utils.PersistentHttpCookieStore
import me.sunzheng.mana.utils.PreferenceManager
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {
    @Provides
    fun providerBanugmiModelDao(database: AppDatabase): BangumiDao {
        return database.bangumiDao()
    }

    @Provides
    fun providerEpisodeDao(database: AppDatabase): EpisodeDao {
        return database.episodeDao()
    }

    @Provides
    fun providerFavriouteDao(database: AppDatabase): FavirouteDao {
        return database.favriouteDao()
    }

    @Provides
    fun providerVideoFileDao(database: AppDatabase): VideoFileDao {
        return database.videoFileDao()
    }

    @Provides
    fun providerApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    fun providerOnAirDao(database: AppDatabase): OnAirDao {
        return database.onAirDao()
    }

    @Provides
    fun providerWatchProgressDao(database: AppDatabase): WatchProgressDao {
        return database.watchProgressDao()
    }

    @Named("userName")
    @Provides
    fun providerUserName(@ApplicationContext context: Context): String {
        return context.getSharedPreferences(
            PreferenceManager.Global.STR_SP_NAME,
            Context.MODE_PRIVATE
        ).getString(PreferenceManager.Global.STR_USERNAME, "") ?: ""
    }

    @Named(PreferenceManager.Global.STR_KEY_HOST)
    @Provides
    fun providerHost(@ApplicationContext context: Context): String {
        return context.getSharedPreferences(
            PreferenceManager.Global.STR_SP_NAME,
            Context.MODE_PRIVATE
        ).getString(PreferenceManager.Global.STR_KEY_HOST, "") ?: ""
    }
}

@Module
@InstallIn(SingletonComponent::class)
object SingletionProvider {
    @Singleton
    @Provides
    fun providerDataBase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "data-base")
            .allowMainThreadQueries()
            .addMigrations(Migration(5, 6) {

            })
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .build()
    }

    @Singleton
    @Provides
    fun providerRetrofit(@ApplicationContext context: Context): Retrofit {
        var isConfigChanged = false
        val configListener =
            SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
                if (key == PreferenceManager.Global.STR_KEY_HOST) {
                    isConfigChanged = true
                }
            }
        var sharedPreferences = context.getSharedPreferences(
            PreferenceManager.Global.STR_SP_NAME,
            MultiDexApplication.MODE_PRIVATE
        ).apply {
            registerOnSharedPreferenceChangeListener(configListener)
        }
        var host = sharedPreferences!!.getString(PreferenceManager.Global.STR_KEY_HOST, "")
        return Retrofit.Builder()
            .baseUrl(host)
            .addCallAdapterFactory(LiveDataCallAdapterFactory())
            .addConverterFactory(GsonConverterFactory.create())
            .client(defaultOkHttpClient(context))
            .build()
    }

    @Singleton
    @Provides
    fun defaultOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        var applicationInfo = context.applicationInfo
        var packageManager = context.packageManager
        var packageName = context.packageName
        var versionCode = try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            if (Build.VERSION.SDK_INT >= 28)
                packageInfo.longVersionCode.toString()
            else
                packageInfo.versionCode.toString()
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "0"
        }

        var applicationLabel = applicationInfo.loadLabel(packageManager).toString()
        var userAgent = "$applicationLabel/$versionCode"
        return OkHttpClient.Builder()
            .connectTimeout(10000, TimeUnit.MILLISECONDS)
            .addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            .addInterceptor { chain ->
                var request = chain.request()
                var headers = request.headers()
                var s = headers["User-Agent"]
                if (TextUtils.isEmpty(s)) {
                    s = userAgent
                } else {
                    s += ";$userAgent"
                }
                headers = headers.newBuilder().set("User-Agent", s).build()
                request = request.newBuilder().headers(headers).build()
                chain.proceed(request)
            }
            .cookieJar(
                JavaNetCookieJar(
                    CookieManager(
                        PersistentHttpCookieStore(context),
                        CookiePolicy.ACCEPT_ALL
                    )
                )
            )
            .build()
    }
}
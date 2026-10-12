package me.sunzheng.mana.core.net.v2.di

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import me.sunzheng.mana.core.net.LiveDataCallAdapterFactory
import me.sunzheng.mana.utils.PreferenceManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 按当前配置的 host 提供 Retrofit（FR-10 Host 热更新）。
 *
 * 原实现把 Retrofit 做成 `@Singleton` 且只在首次创建时读一次 host，导致运行中改 host 不生效
 * （`SingletionProvider.kt` 里的 `isConfigChanged`/`configListener` 是无效逻辑）。
 * 这里每次 [current] 都读一次 SP，host 变化才重建；host 未变则复用实例，避免重复构建。
 */
@Singleton
class RetrofitHolder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    private var cachedHost: String? = null
    private var cachedRetrofit: Retrofit? = null

    @Synchronized
    fun current(): Retrofit {
        val host = context.getSharedPreferences(
            PreferenceManager.Global.STR_SP_NAME,
            Context.MODE_PRIVATE
        ).getString(PreferenceManager.Global.STR_KEY_HOST, "") ?: ""
        val cached = cachedRetrofit
        if (cached != null && host == cachedHost) return cached
        return build(host).also {
            cachedRetrofit = it
            cachedHost = host
        }
    }

    /** 与改造前保持一致的基址语义（host 直接作为 baseUrl）。 */
    private fun build(host: String): Retrofit = Retrofit.Builder()
        .baseUrl(host)
        .addCallAdapterFactory(LiveDataCallAdapterFactory())
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
}

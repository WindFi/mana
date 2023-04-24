package me.sunzheng.mana.core.net.v2

import androidx.lifecycle.LiveData
import me.sunzheng.mana.core.net.ApiResponse
import me.sunzheng.mana.core.net.v2.wrappers.AirWrapper
import me.sunzheng.mana.home.FavoriteStatusRequest
import me.sunzheng.mana.home.bangumi.Response
import me.sunzheng.mana.home.bangumi.WatchProgressResponse
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper
import me.sunzheng.mana.home.episode.wrapper.EpisodeWrapper
import me.sunzheng.mana.home.feedback.FeedbackRequestWrapper
import me.sunzheng.mana.home.main.ResponseWrapper
import me.sunzheng.mana.home.mybangumi.wrapper.FavoriteWrapper
import me.sunzheng.mana.home.search.SearchResultWrapper
import retrofit2.http.*

interface ApiService {
    companion object {
        const val HOME_PATH = "/api/home"
    }

    /**
     * the favorite status, can be one of the following value: 1 (WISH), 2 (WATCHED), 3 (WATCHING), 4 (PAUSE), 5 (ABANDONED) and 0 (all of above) Default: 3
     *
     * @return
     */
    @GET("${HOME_PATH}/my_bangumi")
    fun listMyBangumi(@Query("status") status: Int): LiveData<ApiResponse<FavoriteWrapper>>

    /**
     * List all bangumi which is currently on air, on air means current date is between air_date and the last episode airdate plus one week.
     *
     * @param type 2:新番 6:日剧
     * @return
     */
    @GET("${HOME_PATH}/on_air")
    fun listAllAir(@Query("type") type: Int): LiveData<ApiResponse<AirWrapper>>

    /**
     * List all bangumi base on the given query criteria.
     *
     * @param page  the page number start from 1. Default: 1.
     * @param count count per page, by provide count = -1, client can obtain all data. Default: 10.
     * @param field the field name used for sort by. Default: air_date.
     * @param order the sort order which can be only two value: desc and asc. Default: desc.
     * @param name  the search term for filtering result. this string will be matched by name_cn, name and summary.
     * @return
     */
    @GET("${HOME_PATH}/bangumi")
    fun listAllBangumi(
        @Query("page") page: Int = 0,
        @Query("count") count: Int = 30,
        @Query("sort_field") field: String? = null,
        @Query("sort_order") order: String? = null,
        @Query("name") name: String
    ): LiveData<ApiResponse<SearchResultWrapper>>?

    /**
     * Get the episode by specific id
     *
     * @param id
     * @return
     */
    @GET("${HOME_PATH}/bangumi/{id}")
    fun queryBangumiDetail(@Path("id") id: String): LiveData<ApiResponse<BangumiDetailWrapper>>


    @POST("/api/watch/history/synchronize")
    fun synchronizeEpisodeHistory(@Body request: SynchronizeEpisodeHistoryRequest): LiveData<ApiResponse<WatchProgressResponse>>

    @POST("/api/watch/history/{episode_id}")
    fun updateWatchProgress(
        @Path("episode_id") id: String,
        @Body request: Record
    ): LiveData<ApiResponse<WatchProgressResponse>>

    /**
     * @param id
     * @param request
     * @return
     */
    @POST("/api/watch/favorite/bangumi/{id}")
    fun putBangumiFavoriteStatus(
        @Path("id") id: String,
        @Body request: FavoriteStatusRequest
    ): LiveData<ApiResponse<Response>>

    /**
     * Get the episode by specific id
     *
     * @param id
     * @return
     */
    @GET("${HOME_PATH}/episode/{id}")
    fun queryEpisode(@Path("id") id: String): LiveData<ApiResponse<EpisodeWrapper>>

    /**
     * Send a feedback about current episode and video_file.
     *
     * @return
     */
    @POST("${HOME_PATH}/feedback")
    fun postFeedBack(@Body request: FeedbackRequestWrapper): LiveData<ApiResponse<Response>>

    /**
     * Will only get announcement which current time is between start_time and end_time
     */
    @GET("${HOME_PATH}/announce")
    fun listallAvailable(): LiveData<ApiResponse<ResponseWrapper>>?

    //    Request URL: https://suki.moe/api/home/bangumi?count=-1&order_by=air_date&page=1&sort=desc
    @GET("${HOME_PATH}/bangumi")
    fun listAll(
        @Query("page") page: Int = 0,
        @Query("count") count: Int = -1,
        @Query("order_by") order: String,
        @Query("name") name: String
    ): LiveData<ApiResponse<SearchResult>>
}
package me.sunzheng.mana.videoplayer

import android.support.v4.media.MediaDescriptionCompat
import androidx.lifecycle.*
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import me.sunzheng.mana.core.net.ApiResponse
import me.sunzheng.mana.core.net.ApiSuccessResponse
import me.sunzheng.mana.core.net.Resource
import me.sunzheng.mana.core.net.v2.*
import me.sunzheng.mana.core.net.v2.database.*
import me.sunzheng.mana.home.bangumi.WatchProgressResponse
import me.sunzheng.mana.home.episode.wrapper.EpisodeWrapper
import me.sunzheng.mana.home.main.BangumiRepository
import me.sunzheng.mana.utils.HostUtil
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class VideoPlayerVideoModel @Inject constructor() : ViewModel() {

    @Named("userName")
    @Inject
    lateinit var userName: String

    @Named("host")
    @Inject
    lateinit var host: String

    @Inject
    lateinit var bangumiRepository: BangumiRepository

    lateinit var bangumiId: String

    @Inject
    lateinit var repository: VideoRepository

    val isPlaying = MutableLiveData(false)
    val position = MutableLiveData(-1)
    var isJaFirst = false

    val mediaDescritionLiveData by lazy {
        MutableLiveData<MediaDescriptionCompat>()
    }
    val videoFileLiveData by lazy {
        MutableLiveData<VideoFileEntity>()
    }
    val brighnessLiveData by lazy {
        MutableLiveData<Float>()
    }
    val soundLiveData by lazy {
        MutableLiveData<Float>()
    }
    val isListShowing: MutableLiveData<Boolean> by lazy {
        MutableLiveData(false)
    }

    //    进度条
    val seekPositionLiveData by lazy {
        MutableLiveData<Long>()
    }
    val watchProgressLiveData: MutableLiveData<WatchProgressEntity>? by lazy {
        MutableLiveData()
    }

    fun fetchEpisodeList(id: UUID, status: Int = 2) =
        bangumiRepository.queryEpisodeList(id, status, userName).switchMap { source ->
            liveData {
                val result = Resource.switchMap(source) {
                    it?.map { entity ->
                        entity.episodeEntity.thumbnailImage?.url =
                            HostUtil.makeUp(host, entity.episodeEntity.thumbnailImage?.url)
                        entity.episodeEntity.parseMediaDescription("")
                    } ?: emptyList<MediaDescriptionCompat>()
                }
                emit(result)
            }
        }


    fun fetchVideoFiles(episodeId: UUID) = repository.fetchVideoFiles(episodeId, userName)
    fun fetchWatchProgress(episodeId: UUID) =
        repository.fetchWatchProgress(userName, episodeId)

    fun updateWatchProgress(
        bangumiId: String,
        userName: String = this.userName,
        episodeEntity: EpisodeEntity,
        lastWatchPosition: Float,
        duration: Float,
        watchprocessEntity: WatchProgressEntity? = null
    ) = run {
        var record = watchprocessEntity?.parseRecord() ?: Record()
        record.apply {
            this.userName = userName
            this.bangumiId = bangumiId.toUUID()
            this.episodeId = episodeEntity.id
            this.lastWatchPosition = lastWatchPosition
            this.lastWatchTime = System.currentTimeMillis()
            this.percentage = lastWatchPosition / duration
            this.isFinised = lastWatchPosition >= duration * 0.95
        }
        repository.updateWatchProgresss(userName, record)
    }
}

class VideoRepository {

    lateinit var apiService: ApiService

    lateinit var videoFileDao: VideoFileDao

    lateinit var watchProgressDao: WatchProgressDao

    fun fetchVideoFiles(episodeId: UUID, userName: String) =
        object : NetworkBoundResource<List<VideoFileEntity>?, EpisodeWrapper>() {
            override fun saveCallResult(item: EpisodeWrapper) {
                item.videoFiles?.map {
                    Gson().fromJson(
                        Gson().toJson(it),
                        VideoFileEntity::class.java
                    )
                }?.forEach {
                    videoFileDao.insert(it)
                }
                item.watchProgress?.run {
                    var local = watchProgressDao.queryByEpisodeId(episodeId, userName)
                    var m =
                        Gson().fromJson(Gson().toJson(this), WatchProgressEntity::class.java)
                    m.id = local!!.id
                    m.userName = userName
                    watchProgressDao.insert(m)
                }
            }

            override fun shouldFetch(data: List<VideoFileEntity>?): Boolean = true

            override fun loadFromDb() = liveData {
                var m = videoFileDao.queryByEpisodeId(episodeId)
                emit(
                    m
                )
            }

            override fun createCall(): LiveData<ApiResponse<EpisodeWrapper>> =
                apiService.queryEpisode(episodeId.toString())
        }.asLiveData()

    fun fetchWatchProgress(userName: String, episodeId: UUID) =
        watchProgressDao.queryByEpisodeId(episodeId, userName)

    fun updateWatchProgresss(userName: String, record: Record) =
        object : NetworkBoundResource<WatchProgressEntity?, WatchProgressResponse>() {
            override fun processResponse(response: ApiSuccessResponse<WatchProgressResponse>): WatchProgressResponse {
                return response.body
            }

            override fun saveCallResult(item: WatchProgressResponse) {
                record.takeIf { item.id != null }?.apply { this.id = item.id!!.toUUID() }?.let {
                    it.parseWatchProgressEntity()
                }?.run {
                    watchProgressDao.insert(this)
                }
            }

            override fun shouldFetch(data: WatchProgressEntity?) = true

            override fun loadFromDb() =
                liveData {
                    emit(
                        watchProgressDao.queryByEpisodeId(
                            record.episodeId!!,
                            userName
                        )
                    )
                }

            override fun createCall() = apiService.synchronizeEpisodeHistory(
                SynchronizeEpisodeHistoryRequest(ArrayList<Record>().apply { this.add(record) })
            )
        }.asLiveData()
}
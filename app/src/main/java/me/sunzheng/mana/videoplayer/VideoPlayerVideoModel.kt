package me.sunzheng.mana.videoplayer

import androidx.lifecycle.*
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import me.sunzheng.mana.core.WatchProgress
import me.sunzheng.mana.core.net.ApiResponse
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.NetworkBoundResource
import me.sunzheng.mana.core.net.v2.database.*
import me.sunzheng.mana.home.episode.wrapper.EpisodeWrapper
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class VideoPlayerVideoModel @Inject constructor(private val state: SavedStateHandle) : ViewModel() {
    @Inject
    lateinit var apiService: ApiService

    @Named("userName")
    @Inject
    lateinit var userName: String

    @Inject
    lateinit var videoFileDao: VideoFileDao

    @Inject
    lateinit var watchProgressDao: WatchProgressDao

    val repository: VideoRepository by lazy {
        VideoRepository().also {
            it.apiService = apiService
            it.videoFileDao = videoFileDao
            it.watchProgressDao = watchProgressDao
        }
    }

    // TODO: 需要处理一下savedinstance 的问题 要保存 position items
    val isPlaying = MutableLiveData(false)
    val position = MutableLiveData(0)
    fun fetchVideoFiles(episodeId: UUID) = repository.fetchVideoFiles(episodeId, userName)

    fun updateWatchProgress(userName: String = this.userName, watchProgress: WatchProgress) {

    }
}

class VideoRepository {
    lateinit var apiService: ApiService
    lateinit var videoFileDao: VideoFileDao
    lateinit var watchProgressDao: WatchProgressDao
    fun fetchVideoFiles(episodeId: UUID, userName: String) =
        object : NetworkBoundResource<VideoFileAndWatchProgress?, EpisodeWrapper>() {
            override fun saveCallResult(item: EpisodeWrapper) {
                item.videoFiles.map {
                    Gson().fromJson(
                        Gson().toJson(it),
                        VideoFileEntity::class.java
                    )
                }
                    .run {
                        videoFileDao.insert(*this.toTypedArray())
                    }
                item.watchProgress?.run {
                    var local = watchProgressDao.queryByEpisodeId(episodeId, userName)
                    var m = Gson().fromJson(Gson().toJson(this), WatchProgressEntity::class.java)
                    m.id = local!!.id
                    m.userName = userName
                    watchProgressDao.insert(m)
                }
            }

            override fun shouldFetch(data: VideoFileAndWatchProgress?): Boolean = true

            override fun loadFromDb() = liveData {
                emit(videoFileDao.queryByEpisodeId(episodeId, userName))
            }

            override fun createCall(): LiveData<ApiResponse<EpisodeWrapper>> =
                apiService.queryEpisode(episodeId.toString())
        }.asLiveData()

    fun fetchWatchProgress(userName: String, episodeId: UUID) {

    }

    fun updateWatchProgress(userName: String, episodeId: UUID, record: WatchProgressEntity) {
//        请求体需要转换一下
    }
}
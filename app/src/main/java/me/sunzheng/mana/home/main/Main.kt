package me.sunzheng.mana.home.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import me.sunzheng.mana.core.AnnounceModel
import me.sunzheng.mana.core.net.ApiResponse
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.NetworkBoundResource
import me.sunzheng.mana.core.net.v2.database.*
import me.sunzheng.mana.core.net.v2.wrappers.AirWrapper
import me.sunzheng.mana.home.FavoriteStatusRequest
import me.sunzheng.mana.home.bangumi.Response
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var bangumiDao: BangumiDao

    @Inject
    lateinit var favriouteDao: FavirouteDao

    @Inject
    lateinit var onAirDao: OnAirDao

    @Inject
    lateinit var episodeDao: EpisodeDao

    @Named("userName")
    @Inject
    lateinit var userName: String

    val repository: BangumiRepository by lazy {
        BangumiRepository().also {
            it.apiService = apiService
            it.bangumiDao = bangumiDao
            it.favriouteDao = favriouteDao
            it.episodeDao = episodeDao
            it.onAirDao = onAirDao
        }
    }
    val announceLiveData: LiveData<AnnounceModel> by lazy {
        MutableLiveData<AnnounceModel>()
    }

    fun queryAir(type: Int = 0, userName: String = this.userName) =
        repository.queryOnAir(type, userName)
}

class BangumiRepository {
    lateinit var apiService: ApiService
    lateinit var bangumiDao: BangumiDao
    lateinit var favriouteDao: FavirouteDao
    lateinit var episodeDao: EpisodeDao
    lateinit var onAirDao: OnAirDao
    lateinit var watchProgressDao: WatchProgressDao
    fun queryOnAir(type: Int, userName: String) =
        object : NetworkBoundResource<List<BangumiEntity>, AirWrapper>() {
            override fun saveCallResult(item: AirWrapper) {
                item.data?.forEach {
                    var bangumiEntity =
                        Gson().fromJson(Gson().toJson(it), BangumiEntity::class.java)
                    var favriouteEntity = FavriouteEntity(
                        bangumiId = bangumiEntity.id,
                        status = it.favoriteStatus,
                        userName = userName,
                        unwatched_count = it.unwatched_count
                    )
                    bangumiDao.insert(bangumiEntity)
                    var source =
                        favriouteDao.queryByBangumiId(bangumiEntity.id, userName)?.let { dateView ->
                            dateView.state.status = it.favoriteStatus
                            dateView.state.unwatched_count = it.unwatched_count
                            dateView.state
                        }
                    favriouteDao.insert(source ?: favriouteEntity)
                }
                item.data?.map { RelationOnAir(it.id) }?.run {
                    onAirDao.deleteAll()
                    onAirDao.insert(*this.toTypedArray())
                }
            }

            override fun shouldFetch(data: List<BangumiEntity>?): Boolean = true
            override fun loadFromDb(): LiveData<List<BangumiEntity>> = liveData {
                emit(onAirDao.queryList(type))
            }
            override fun createCall() = apiService.listAllAir(type)
        }.asLiveData()

    fun queryEpisodeList(id: UUID, status: Int, userName: String) =
        object : NetworkBoundResource<List<EpisodeAndWatchprogress>, BangumiDetailWrapper>() {
            override fun saveCallResult(item: BangumiDetailWrapper) {
                bangumiDao.insert(
                    Gson().fromJson(
                        Gson().toJson(item.bangumiDetails),
                        BangumiEntity::class.java
                    )
                )
                item.bangumiDetails.let { entity ->
                    var source =
                        favriouteDao.queryByBangumiId(id, userName)?.let { it.state }?.apply {
                            this.status = entity.favoriteStatus
                            unwatched_count = entity.unwatched_count
                        }
                    var model = source ?: FavriouteEntity(
                        bangumiId = id,
                        status = entity.favoriteStatus,
                        userName = userName,
                        unwatched_count = entity.unwatched_count
                    )
                    favriouteDao.insert(model)
                }
                item.bangumiDetails.episodes.map {
                    Gson().fromJson(
                        Gson().toJson(it),
                        EpisodeEntity::class.java
                    ) to it.watchProgress?.let {
                        Gson().fromJson(
                            Gson().toJson(it),
                            WatchProgressEntity::class.java
                        ) ?: null
                    }
                }.forEach {
                    // TODO:  这里的代码需要优化一下
//               ============================================这里需要优化========================================
                    episodeDao.insert(it.first)
                    var em = watchProgressDao.queryByEpisodeId(it.first.id, userName)?.let { old ->
                        it.second?.apply {
                            this._id = old._id
                            this.userName = userName
                        }
                    } ?: it.second?.apply {
                        this.userName = userName
                    }
                    em?.run {
                        watchProgressDao.insert(em)
                    }
//                    =================================================到这里为止====================================
                }

            }

            override fun shouldFetch(data: List<EpisodeAndWatchprogress>?): Boolean = true

            override fun loadFromDb(): LiveData<List<EpisodeAndWatchprogress>> =
                liveData { emit(episodeDao.queryListByBangumiId(id, status, userName)) }


            override fun createCall(): LiveData<ApiResponse<BangumiDetailWrapper>> =
                apiService.queryBangumiDetail(id.toString())
        }.asLiveData()

    fun queryBangumiAndFavriote(id: UUID, userName: String) =
        liveData { emit(favriouteDao.queryByBangumiId(id, userName)) }


    fun updateBangumiFavrioteState(id: UUID, status: Int, userName: String) =
        object : NetworkBoundResource<Int, Response>() {
            override fun saveCallResult(item: Response) {
                when (item.status) {
                    0L -> {
                        var source = favriouteDao.queryByBangumiId(id, userName)
                        var model = source?.state ?: FavriouteEntity(id, status, userName)
                        model.apply {
                            bangumiId = id
                            this.status = status
                            this.userName = userName
                        }
                        favriouteDao.insert(model)
                    }
                }
            }

            override fun shouldFetch(data: Int?): Boolean = true

            override fun loadFromDb(): LiveData<Int> = liveData {
                emit(favriouteDao.queryByBangumiId(id, userName)?.state?.status ?: 0)
            }

            override fun createCall(): LiveData<ApiResponse<Response>> =
                apiService.putBangumiFavoriteStatus(id.toString(),
                    FavoriteStatusRequest().apply { this.status = status }
                )
        }.asLiveData()
    // TODO: 2021/12/4 updateWatchProgress
//    fun updateWatchProgress(vararg watchProgressEntity: WatchProgressEntity,userName:String,sync:Boolean=false)=object:NetworkBoundResource<WatchProgressEntity,Response>(){
//        override fun saveCallResult(item: Response) {
//            TODO("Not yet implemented")
//        }
//
//        override fun shouldFetch(data: WatchProgressEntity?)=sync
//
//        override fun loadFromDb(): LiveData<WatchProgressEntity> {
//            TODO("Not yet implemented")
//        }
//
//        override fun createCall(): LiveData<ApiResponse<Response>> {
//            TODO("Not yet implemented")
//        }
//    }
}

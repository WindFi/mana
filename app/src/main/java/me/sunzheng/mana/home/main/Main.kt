package me.sunzheng.mana.home.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import me.sunzheng.mana.core.AnnounceModel
import me.sunzheng.mana.core.net.ApiResponse
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.NetworkBoundResource
import me.sunzheng.mana.core.net.v2.database.*
import me.sunzheng.mana.core.net.v2.wrappers.AirWrapper
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var bangumiDao: BangumiDao

    @Inject
    lateinit var favriouteDao: FavirouteDao

    @Inject
    lateinit var episodeDao: EpisodeDao
    val repository: BangumiRepository by lazy {
        BangumiRepository().also {
            it.apiService = apiService
            it.bangumiDao = bangumiDao
            it.favriouteDao = favriouteDao
            it.episodeDao = episodeDao
        }
    }
    val announceLiveData: LiveData<AnnounceModel> by lazy {
        MutableLiveData<AnnounceModel>()
    }

    fun queryAir(type: Int = 0) = repository.queryOnAir(type, status = 1, "SunZheng")
}

class BangumiRepository {
    lateinit var apiService: ApiService
    lateinit var bangumiDao: BangumiDao
    lateinit var favriouteDao: FavirouteDao
    lateinit var episodeDao: EpisodeDao

    fun queryOnAir(type: Int, status: Int = 0, userName: String) =
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
                    favriouteDao.insert(favriouteEntity)
                }
            }

            override fun shouldFetch(data: List<BangumiEntity>?): Boolean = true

            override fun loadFromDb(): LiveData<List<BangumiEntity>> = bangumiDao.queryList(type)

            override fun createCall() = apiService.listAllAir(type)
        }.asLiveData()

    fun queryEpisodeList(id: UUID, status: Int, userName: String) =
        object : NetworkBoundResource<List<EpisodeEntity>, BangumiDetailWrapper>() {
            override fun saveCallResult(item: BangumiDetailWrapper) {
                item.bangumiDetails.episodes.map {
                    Gson().fromJson(Gson().toJson(it), EpisodeEntity::class.java)
                }.forEach {
                    episodeDao.insert(it)
                }
                bangumiDao.insert(
                    Gson().fromJson(
                        Gson().toJson(item.bangumiDetails),
                        BangumiEntity::class.java
                    )
                )
                item.bangumiDetails.let {
                    FavriouteEntity(
                        bangumiId = UUID.fromString(it.id),
                        status = it.favoriteStatus,
                        userName = userName,
                        unwatched_count = it.unwatched_count
                    )
                }.run {
                    favriouteDao.insert(this)
                }
            }

            override fun shouldFetch(data: List<EpisodeEntity>?): Boolean = true

            override fun loadFromDb(): LiveData<List<EpisodeEntity>> =
                episodeDao.queryListByBangumiId(id, status)

            override fun createCall(): LiveData<ApiResponse<BangumiDetailWrapper>> =
                apiService.queryBangumiDetail(id.toString())
        }.asLiveData()
    // TODO: 2021/12/4
//    fun queryEpisodeAndProgress(id:UUID,userId:String)
    // TODO: 2021/12/4 updateFavrioute 
    // TODO: 2021/12/4 updateWatchProgress 
}

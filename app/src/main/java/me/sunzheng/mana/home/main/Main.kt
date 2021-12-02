package me.sunzheng.mana.home.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import me.sunzheng.mana.core.AnnounceModel
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.NetworkBoundResource
import me.sunzheng.mana.core.net.v2.database.BangumiDao
import me.sunzheng.mana.core.net.v2.database.BangumiEntity
import me.sunzheng.mana.core.net.v2.database.FavirouteDao
import me.sunzheng.mana.core.net.v2.database.FavriouteEntity
import me.sunzheng.mana.core.net.v2.wrappers.AirWrapper
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {
    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var bangumiDao: BangumiDao

    @Inject
    lateinit var favriouteDao: FavirouteDao
    val repository: BangumiRepository by lazy {
        BangumiRepository().also {
            it.apiService = apiService
            it.bangumiDao = bangumiDao
            it.favriouteDao = favriouteDao
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
}

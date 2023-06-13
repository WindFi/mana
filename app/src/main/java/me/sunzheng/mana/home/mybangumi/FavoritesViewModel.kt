package me.sunzheng.mana.home.mybangumi

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import me.sunzheng.mana.core.net.ApiResponse
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.NetworkBoundResource
import me.sunzheng.mana.core.net.v2.database.*
import me.sunzheng.mana.home.mybangumi.wrapper.FavoriteWrapper
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MyFavoriteViewModel @Inject constructor() : ViewModel() {
    @Inject
    lateinit var favriouteDao: FavirouteDao

    @Inject
    lateinit var bangumiDao: BangumiDao

    @Inject
    lateinit var apiService: ApiService

    val positionLiveData: MutableLiveData<Int> by lazy {
        MutableLiveData()
    }

    @Named("userName")
    @Inject
    lateinit var userName: String
    val repository: FavoritesRepository by lazy {
        FavoritesRepository().also {
            it.favriouteDao = favriouteDao
            it.bangumiDao = bangumiDao
            it.apiService = apiService
            it.userName = userName
        }
    }

    fun filter(status: Int = 0, page: Int = 0) = repository.query(status, page)
}

class FavoritesRepository {
    lateinit var favriouteDao: FavirouteDao
    lateinit var bangumiDao: BangumiDao
    lateinit var apiService: ApiService
    lateinit var userName: String
    fun query(status: Int, page: Int) =
        object : NetworkBoundResource<List<BangumiAndFavorites>, FavoriteWrapper>() {
            override fun saveCallResult(item: FavoriteWrapper) {
                item.data?.forEach {
                    bangumiDao.insert(Gson().fromJson(Gson().toJson(it), BangumiEntity::class.java))
                }
                item.data.forEach {
                    var source = favriouteDao.queryByBangumiId(UUID.fromString(it.id), userName)
                        ?.let { dataView ->
                            dataView.state.apply {
                                this.status = it.favoriteStatus
                                unwatched_count = it.unwatched_count
                            }
                        } ?: FavriouteEntity(
                        UUID.fromString(it.id),
                        status = it.favoriteStatus,
                        userName = userName,
                        it.unwatched_count
                    )
                    favriouteDao.insert(source)
                }
            }

            override fun shouldFetch(data: List<BangumiAndFavorites>?): Boolean = true

            override fun loadFromDb(): LiveData<List<BangumiAndFavorites>> = liveData {
                emit(
                    if (status != 0) favriouteDao.queryBangumiList(
                        status,
                        userName
                    ) else favriouteDao.queryBangumiListAll(status, userName)
                )
            }

            override fun createCall(): LiveData<ApiResponse<FavoriteWrapper>> =
                apiService.listMyBangumi(status)
        }.asLiveData()
}
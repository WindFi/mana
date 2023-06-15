package me.sunzheng.mana.home.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import me.sunzheng.mana.core.net.Resource
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.NetworkBoundResource
import me.sunzheng.mana.core.net.v2.SearchResult
import me.sunzheng.mana.core.net.v2.database.BangumiDao
import me.sunzheng.mana.core.net.v2.database.BangumiEntity
import me.sunzheng.mana.core.net.v2.database.FavirouteDao
import me.sunzheng.mana.core.net.v2.database.FavriouteEntity
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var bangumiDao: BangumiDao

    @Inject
    lateinit var favirouteDao: FavirouteDao

    @Named("userName")
    @Inject
    lateinit var userName: String
    var count = -1
    var page = 1
    lateinit var key: String
    val repository: SearchRepository by lazy {
        SearchRepository().also {
            it.apiService = apiService
            it.bangumiDao = bangumiDao
            it.userName = userName
            it.favriouteDao = favirouteDao
        }
    }

    fun query(word: String) = liveData<Resource<List<BangumiEntity>>> {
        key = word
        page = 1
        count = 30
        emitSource(repository.query(key, count, page))
    }

    fun loadMore() = repository.query(key, count, page++)
}

class SearchRepository {
    lateinit var apiService: ApiService
    lateinit var bangumiDao: BangumiDao
    lateinit var favriouteDao: FavirouteDao
    lateinit var userName: String
    fun query(word: String, count: Int = -1, page: Int) =
        object : NetworkBoundResource<List<BangumiEntity>, SearchResult>() {

            override fun saveCallResult(item: SearchResult) {
                val total = item.total
                val bangumiList =
                    item.data.map { Gson().fromJson(Gson().toJson(it), BangumiEntity::class.java) }
                if (total >= 0) {
                    val data = bangumiList.toTypedArray()
                    bangumiDao.insert(*data)

                    item.data.forEach {
                        val source =
                            favriouteDao.queryByBangumiId(it.id, userName)?.let { dateView ->
                                dateView.state.status = it.favoriteStatus
                                dateView.state.unwatched_count = it.unwatched_count
                                dateView.state
                            }
                        val favriouteEntity = FavriouteEntity(
                            bangumiId = it.id,
                            status = it.favoriteStatus,
                            userName = userName,
                            unwatched_count = it.unwatched_count
                        )
                        favriouteDao.insert(source ?: favriouteEntity)
                    }
                }
            }

            override fun shouldFetch(data: List<BangumiEntity>?) = true

            override fun loadFromDb(): LiveData<List<BangumiEntity>> = liveData {
                val _count = if (count == -1) 30 else count
                val _page = if (page <= 1) 0 else page - 1
                emit(bangumiDao.query(word = word, maxLimit = _count, offset = _page * _count))
            }

            override fun createCall() =
                apiService.listAll(page, count = count, order = "air_date", name = word)
        }.asLiveData()
}
package me.sunzheng.mana.home.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.NetworkBoundResource
import me.sunzheng.mana.core.net.v2.SearchResult
import me.sunzheng.mana.core.net.v2.database.BangumiDao
import me.sunzheng.mana.core.net.v2.database.BangumiEntity
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var bangumiDao: BangumiDao
    var count = -1
    var page = 1
    lateinit var key: String
    val repository: SearchRepository by lazy {
        SearchRepository().also {
            it.apiService = apiService
            it.bangumiDao = bangumiDao
        }
    }

    fun query(word: String) = liveData {
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
    fun query(word: String, count: Int = -1, page: Int) =
        object : NetworkBoundResource<List<BangumiEntity>, SearchResult>() {

            override fun saveCallResult(item: SearchResult) {
                var total = item.total
                if (total >= 0) {
                    var data = item.data.toTypedArray()
                    bangumiDao.insert(*data)
                }
            }

            override fun shouldFetch(data: List<BangumiEntity>?) = true

            override fun loadFromDb(): LiveData<List<BangumiEntity>> = liveData {
                var _count = if (count == -1) 30 else count
                var _page = if (page <= 1) 0 else page - 1
                emit(bangumiDao.query(word = word, maxLimit = _count, offset = _page * _count))
            }

            override fun createCall() =
                apiService.listAll(page, count = count, order = "air_date", name = word)
        }.asLiveData()
}
package me.sunzheng.mana.home.bangumi

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.database.BangumiDao
import me.sunzheng.mana.core.net.v2.database.EpisodeDao
import me.sunzheng.mana.core.net.v2.database.FavirouteDao
import me.sunzheng.mana.home.main.BangumiRepository
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BangumiDetailsViewModel @Inject constructor() : ViewModel() {
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

    fun fetchEpisodeList(bangumiId: UUID, userName: String) =
        repository.queryEpisodeList(bangumiId, 2, userName)
}

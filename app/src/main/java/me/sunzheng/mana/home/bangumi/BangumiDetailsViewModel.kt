package me.sunzheng.mana.home.bangumi

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import me.sunzheng.mana.home.main.BangumiRepository
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class BangumiDetailsViewModel @Inject constructor() : ViewModel() {

    @Named("userName")
    @Inject
    lateinit var userName: String

    @Inject
    lateinit var repository: BangumiRepository

    fun fetchEpisodeList(bangumiId: UUID, userName: String) =
        repository.queryEpisodeList(bangumiId, 2, userName)

    fun updateBangumiFavoriteState(bangumiId: UUID, status: Int, userName: String = this.userName) =
        repository.updateBangumiFavrioteState(bangumiId, status, userName)

    fun queryBangumiAndFavorite(bangumiId: UUID, userName: String) =
        repository.queryBangumiAndFavriote(bangumiId, userName)
}

package me.sunzheng.mana.core.net.v2.database

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.android.parcel.Parcelize

@Parcelize
data class BangumiAndFavorites(
    @Relation(
        parentColumn = "bangumiId", entityColumn = "id"
    )
    var entity: BangumiEntity,
    @Embedded var state: FavriouteEntity
) : Parcelable


@Parcelize
data class EpisodeAndWatchprogress(
    @Relation(
        parentColumn = "id", entityColumn = "episodeId"
    )
    var episodeEntity: EpisodeEntity,
    @Embedded
    var watchProgress: WatchProgressEntity? = null
) : Parcelable
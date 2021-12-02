package me.sunzheng.mana.core.net.v2.database

import androidx.room.Embedded
import androidx.room.Relation

data class BangumiAndFavorites(
    @Relation(
        parentColumn = "bangumiId", entityColumn = "id"
    )
    var bangumiEntity: BangumiEntity,
    @Embedded var favriouteState: FavriouteEntity
)
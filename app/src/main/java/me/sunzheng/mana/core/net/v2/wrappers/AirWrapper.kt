package me.sunzheng.mana.core.net.v2.wrappers

import com.google.gson.annotations.SerializedName
import me.sunzheng.mana.core.net.v2.database.BangumiModel


data class AirWrapper(
    @SerializedName("data")
    val data: List<BangumiModel>?
)
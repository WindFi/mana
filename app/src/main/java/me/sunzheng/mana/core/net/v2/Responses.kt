package me.sunzheng.mana.core.net.v2

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import me.sunzheng.mana.core.net.v2.database.BangumiModel


@Parcelize
data class SearchResult(
    @SerializedName("total")
    @Expose
    val total: Int,
    @SerializedName("data")
    @Expose
    val data: List<BangumiModel>
) : Parcelable
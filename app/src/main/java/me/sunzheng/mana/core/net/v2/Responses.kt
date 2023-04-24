package me.sunzheng.mana.core.net.v2

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import me.sunzheng.mana.core.net.v2.database.BangumiEntity


@Parcelize
data class SearchResult(
    @SerializedName("total")
    @Expose
    val total: Int,
    @SerializedName("data")
    @Expose
    val data: List<BangumiEntity>
) : Parcelable
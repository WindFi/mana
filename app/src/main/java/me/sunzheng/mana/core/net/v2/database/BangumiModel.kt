package me.sunzheng.mana.core.net.v2.database

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.annotation.NonNull
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import me.sunzheng.mana.core.CoverImage
import java.util.*

@Keep
@Parcelize
data class BangumiModel(
    @NonNull
    @SerializedName("id")
    @Expose
    var id: UUID,

    @SerializedName("bgm_id")
    @Expose

    var bgmId: Int = 0,


    @SerializedName("name")
    @Expose
    var name: String? = null,


    @SerializedName("name_cn")
    @Expose
    var nameCn: String? = null,


    @SerializedName("summary")
    @Expose
    var summary: String? = null,


    @SerializedName("image")
    @Expose
    var image: String? = null,


    @SerializedName("cover")
    @Expose
    var cover: String? = null,


    @SerializedName("cover_color")
    @Expose
    var cover_color: String? = null,


    @SerializedName("create_time")
    @Expose
    var createTime: Long = 0,


    @SerializedName("update_time")
    @Expose
    var updateTime: Long = 0,


    @SerializedName("eps_no_offset")
    @Expose
    var epsNoOffset: Long = 0,


    @SerializedName("bangumi_moe")
    @Expose
    var bangumiMoe: String? = null,


    @SerializedName("libyk_so")
    @Expose
    var libykSo: String? = null,


    @SerializedName("dmhy")
    @Expose
    var dmhy: String? = null,


    @SerializedName("type")
    @Expose
    var type:Int = 0,


    @SerializedName("status")
    @Expose
    var status:Int = 0,


    @SerializedName("air_date")
    @Expose
    var airDate: String? = null,


    @SerializedName("air_weekday")
    @Expose
    var airWeekday: Long = 0,


    @SerializedName("delete_mark")
    @Expose
    var deleteMark: Long = 0,


    @SerializedName("acg_rip")
    @Expose
    var acgRip: String? = null,


    @SerializedName("rss")
    @Expose
    var rss: String? = null,


    @SerializedName("eps_regex")
    @Expose
    var epsRegex: String? = null,


    @SerializedName("eps")
    @Expose
    var eps: Int = 0,


    @SerializedName("favorite_status")
    @Expose
    var favoriteStatus: Int = 0,


    @SerializedName("unwatched_count")
    @Expose
    var unwatched_count: Int = 0,

    @SerializedName("cover_image")
    @Expose
    var coverImage: CoverImage? = null
) : Parcelable
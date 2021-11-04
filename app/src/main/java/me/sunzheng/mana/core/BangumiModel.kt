package me.sunzheng.mana.core

import com.j256.ormlite.table.DatabaseTable
import android.os.Parcelable
import com.j256.ormlite.field.DatabaseField
import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.Expose
import me.sunzheng.mana.core.CoverImage
import android.os.Parcel
import org.apache.commons.lang3.builder.ToStringBuilder
import me.sunzheng.mana.core.BangumiModel
import android.os.Parcelable.Creator
import com.j256.ormlite.field.DataType
import java.util.*

@DatabaseTable(tableName = "favorites")
data class BangumiModel(
    @DatabaseField(generatedId = true)
    var _id: Long = 0,

    @DatabaseField(columnName = "id", dataType = DataType.UUID)
    @SerializedName("id")
    @Expose
    var id: UUID? = null,

    @DatabaseField(columnName = "bgm_id", dataType = DataType.INTEGER)
    @SerializedName("bgm_id")
    @Expose
    var bgmId:Int = 0,

    @DatabaseField(columnName = "name")
    @SerializedName("name")
    @Expose
    var name: String? = null,

    @DatabaseField(columnName = "name_cn")
    @SerializedName("name_cn")
    @Expose
    var nameCn: String? = null,

    @DatabaseField(columnName = "summary")
    @SerializedName("summary")
    @Expose
    var summary: String? = null,

    @DatabaseField(columnName = "image")
    @SerializedName("image")
    @Expose
    var image: String? = null,

    @DatabaseField(columnName = "cover")
    @SerializedName("cover")
    @Expose
    var cover: String? = null,

    @DatabaseField(columnName = "cover_color")
    @SerializedName("cover_color")
    @Expose
    var cover_color: String? = null,

    @DatabaseField(columnName = "create_time", dataType = DataType.LONG)
    @SerializedName("create_time")
    @Expose
    var createTime: Long = 0,

    @DatabaseField(columnName = "update_time", dataType = DataType.LONG)
    @SerializedName("update_time")
    @Expose
    var updateTime: Long = 0,

    @DatabaseField(columnName = "eps_no_offset", dataType = DataType.LONG)
    @SerializedName("eps_no_offset")
    @Expose
    var epsNoOffset: Long = 0,

    @DatabaseField(columnName = "bangumi_moe")
    @SerializedName("bangumi_moe")
    @Expose
    var bangumiMoe: String? = null,

    @DatabaseField(columnName = "libyk_so")
    @SerializedName("libyk_so")
    @Expose
    var libykSo: String? = null,

    @DatabaseField(columnName = "dmhy")
    @SerializedName("dmhy")
    @Expose
    var dmhy: String? = null,

    @DatabaseField(columnName = "type", dataType = DataType.INTEGER)
    @SerializedName("type")
    @Expose
    var type:Int = 0,

    @DatabaseField(columnName = "status", dataType = DataType.INTEGER)
    @SerializedName("status")
    @Expose
    var status:Int = 0,

    @DatabaseField(columnName = "air_date")
    @SerializedName("air_date")
    @Expose
    var airDate: String? = null,

    @DatabaseField(columnName = "air_weekday", dataType = DataType.LONG)
    @SerializedName("air_weekday")
    @Expose
    var airWeekday: Long = 0,

    @DatabaseField(columnName = "delete_mark", dataType = DataType.LONG)
    @SerializedName("delete_mark")
    @Expose
    var deleteMark: Long = 0,

    @DatabaseField(columnName = "acg_rip")
    @SerializedName("acg_rip")
    @Expose
    var acgRip: String? = null,

    @DatabaseField(columnName = "rss")
    @SerializedName("rss")
    @Expose
    var rss: String? = null,

    @DatabaseField(columnName = "eps_regex")
    @SerializedName("eps_regex")
    @Expose
    var epsRegex: String? = null,

    @DatabaseField(columnName = "eps", dataType = DataType.INTEGER)
    @SerializedName("eps")
    @Expose
    var eps:Int=0,

    @DatabaseField(columnName = "favorite_status", dataType = DataType.INTEGER)
    @SerializedName("favorite_status")
    @Expose
    var favoriteStatus:Int = 0,

    @DatabaseField(columnName = "unwatched_count", dataType = DataType.INTEGER)
    @SerializedName("unwatched_count")
    @Expose
    var unwatched_count:Int= 0,

    @SerializedName("cover_image")
    @Expose
    var coverImage: CoverImage? = null)
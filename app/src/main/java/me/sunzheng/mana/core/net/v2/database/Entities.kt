package me.sunzheng.mana.core.net.v2.database

import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import me.sunzheng.mana.core.CoverImage
import java.util.*

@Parcelize
@Entity(tableName = "bangumi")
data class BangumiEntity(
    @PrimaryKey
    @ColumnInfo
    val id: UUID,
    @SerializedName("bgm_id")
    @Expose
    @ColumnInfo
    var bgmId: Int = 0,

    @ColumnInfo
    @SerializedName("name")
    @Expose
    var name: String? = null,

    @ColumnInfo
    @SerializedName("name_cn")
    @Expose
    var nameCn: String? = null,

    @ColumnInfo
    @SerializedName("summary")
    @Expose
    var summary: String? = null,

    @ColumnInfo
    @SerializedName("image")
    @Expose
    var image: String? = null,

    @ColumnInfo
    @SerializedName("cover")
    @Expose
    var cover: String? = null,

    @ColumnInfo
    @SerializedName("cover_color")
    @Expose
    var cover_color: String? = null,

    @ColumnInfo
    @SerializedName("create_time")
    @Expose
    var createTime: Long = 0,

    @ColumnInfo
    @SerializedName("update_time")
    @Expose
    var updateTime: Long = 0,

    @ColumnInfo
    @SerializedName("eps_no_offset")
    @Expose
    var epsNoOffset: Long = 0,

    @ColumnInfo
    @SerializedName("bangumi_moe")
    @Expose
    var bangumiMoe: String? = null,

    @ColumnInfo
    @SerializedName("libyk_so")
    @Expose
    var libykSo: String? = null,

    @ColumnInfo
    @SerializedName("dmhy")
    @Expose
    var dmhy: String? = null,

    @ColumnInfo
    @SerializedName("type")
    @Expose
    var type: Int = 0,

    @ColumnInfo
    @SerializedName("air_date")
    @Expose
    var airDate: String? = null,

    @ColumnInfo
    @SerializedName("air_weekday")
    @Expose
    var airWeekday: Long = 0,

    @ColumnInfo
    @SerializedName("delete_mark")
    @Expose
    var deleteMark: Long = 0,

    @ColumnInfo
    @SerializedName("acg_rip")
    @Expose
    var acgRip: String? = null,

    @ColumnInfo
    @SerializedName("rss")
    @Expose
    var rss: String? = null,

    @ColumnInfo
    @SerializedName("eps_regex")
    @Expose
    var epsRegex: String? = null,

    @ColumnInfo
    @SerializedName("eps")
    @Expose
    var eps: Int = 0,

    @ColumnInfo
    @SerializedName("status")
    @Expose
    var status: Int = 0,

    @SerializedName("cover_image")
    @Expose
    var coverImage: CoverImage? = null
) : Parcelable

@Parcelize
@Entity(
    tableName = "favorite",
    foreignKeys = [ForeignKey(
        entity = BangumiEntity::class,
        parentColumns = ["id"],
        childColumns = ["bangumiId"]
    )], indices = [Index("bangumiId")]
)
data class FavriouteEntity(
    @ColumnInfo var bangumiId: UUID,
    @ColumnInfo var status: Int,
    @ColumnInfo var userName: String,
    @ColumnInfo
    var unwatched_count: Int = 0,
) : Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo
    var id: Long = 0
}

@Parcelize
@Entity(
    tableName = "episode",
    foreignKeys = [ForeignKey(
        entity = BangumiEntity::class,
        parentColumns = ["id"],
        childColumns = ["bangumiId"]
    )],
    indices = [Index("bangumiId")]
)
data class EpisodeEntity(
    @PrimaryKey
    val id: UUID,
    @ColumnInfo
    @SerializedName("status")
    @Expose
    var status: Long = 0,
    @ColumnInfo
    @SerializedName("episode_no")
    @Expose
    val episodeNo: Int = 0,
    @ColumnInfo
    @SerializedName("update_time")
    @Expose
    val updateTime: Long = 0,
    @ColumnInfo
    @SerializedName("name")
    @Expose
    val name: String? = null,
    @ColumnInfo
    @SerializedName("bgm_eps_id")
    @Expose
    val bgmEpsId: Long = 0,
    @ColumnInfo
    @SerializedName("bangumi_id")
    @Expose
    val bangumiId: UUID? = null,
    @ColumnInfo
    @SerializedName("airdate")
    @Expose
    val airdate: String? = null,
    @ColumnInfo
    @SerializedName("name_cn")
    @Expose
    val nameCn: String? = null,
    @ColumnInfo
    @SerializedName("thumbnail")
    @Expose
    val thumbnail: String? = null,
    @ColumnInfo
    @SerializedName("thumbnail_color")
    @Expose
    val thumbnailColor: String? = null,
    @ColumnInfo
    @SerializedName("create_time")
    @Expose
    val createTime: Long = 0,
    @ColumnInfo
    @SerializedName("duration")
    @Expose
    val duration: String? = null,
    @ColumnInfo
    @SerializedName("thumbnail_image")
    @Expose
    val thumbnailImage: CoverImage? = null
) : Parcelable

@Parcelize
@Entity(
    tableName = "videofile", foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"]
        ),
        ForeignKey(
            entity = BangumiEntity::class,
            parentColumns = ["id"],
            childColumns = ["bangumiId"]
        )],
    indices = [Index("episodeId"), Index("bangumiId")]
)
data class VideoFileEntity(
    @PrimaryKey
    val id: UUID,
    @SerializedName("status")
    @Expose
    val status: Long = 0,

    @SerializedName("torrent_id")
    @Expose
    val torrentId: String? = null,

    @SerializedName("url")
    @Expose
    val url: String? = null,

    @SerializedName("file_path")
    @Expose
    val filePath: String? = null,

    @SerializedName("file_name")
    @Expose
    val fileName: String? = null,

    @SerializedName("resolution_w")
    @Expose
    val resolutionW: Long = 0,

    @SerializedName("download_url")
    @Expose
    val downloadUrl: String? = null,

    @SerializedName("episode_id")
    @Expose
    val episodeId: UUID? = null,

    @SerializedName("resolution_h")
    @Expose
    val resolutionH: Long = 0,

    @SerializedName("bangumi_id")
    @Expose
    val bangumiId: UUID? = null,

    @SerializedName("duration")
    @Expose
    val duration: Long = 0,

    @SerializedName("label")
    @Expose
    val label: String? = null,
) : Parcelable {
    override fun toString(): String {
        return this.label ?: ""
    }
}

@Parcelize
@Entity(
    tableName = "watchprogress", foreignKeys = [
        ForeignKey(
            entity = EpisodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"]
        ),
        ForeignKey(
            entity = BangumiEntity::class,
            parentColumns = ["id"],
            childColumns = ["bangumiId"]
        )],
    indices = [Index("episodeId"), Index("bangumiId")]
)

data class WatchProgressEntity(
    @PrimaryKey
    @SerializedName("id")
    @Expose
    @ColumnInfo(name = "rid")
    var id: UUID,
    @SerializedName("user_id")
    @Expose
    var userId: UUID? = null,
    @ColumnInfo
    var userName: String? = null,
    @SerializedName("last_watch_position")
    @Expose
    @ColumnInfo
    var lastWatchPosition: Float = 0.0f,

    @SerializedName("bangumi_id")
    @Expose
    @ColumnInfo
    var bangumiId: UUID? = null,

    @SerializedName("watch_status")
    @Expose
    @ColumnInfo
    var watchStatus: Long = 0,

    @SerializedName("episode_id")
    @Expose
    @ColumnInfo
    var episodeId: UUID? = null,

    @SerializedName("percentage")
    @Expose
    @ColumnInfo
    var percentage: Float = 0.0f,

    @SerializedName("last_watch_time")
    @Expose
    @ColumnInfo
    var lastWatchTime: Float = 0.0f,
    @SerializedName("is_finished")
    @Expose
    @ColumnInfo
    var isFinised: Boolean = false
) : Parcelable

@Parcelize
@Entity(
    tableName = "OnAir", foreignKeys = [ForeignKey(
        entity = BangumiEntity::class,
        parentColumns = ["id"],
        childColumns = ["bangumiId"]
    )], indices = [Index("bangumiId")]
)
data class RelationOnAir(
    @PrimaryKey
    @ColumnInfo
    var bangumiId: UUID
) : Parcelable
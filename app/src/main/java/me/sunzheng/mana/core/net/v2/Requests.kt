package me.sunzheng.mana.core.net.v2

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class SynchronizeEpisodeHistoryRequest(
    @SerializedName("records")
    val items: List<Record>
) : Parcelable

@Parcelize
data class Record(
    @SerializedName("id")
    @Expose
    var id: UUID? = null,
    @SerializedName("user_id")
    @Expose
    var userId: UUID? = null,
    var userName: String? = null,
    @SerializedName("last_watch_position")
    @Expose
    var lastWatchPosition: Float = 0.0f,

    @SerializedName("bangumi_id")
    @Expose
    var bangumiId: UUID? = null,

    @SerializedName("watch_status")
    @Expose
    var watchStatus: Long = 0,

    @SerializedName("episode_id")
    @Expose
    var episodeId: UUID? = null,

    @SerializedName("percentage")
    @Expose
    var percentage: Float = 0.0f,

    @SerializedName("last_watch_time")
    @Expose
    var lastWatchTime: Long = 0,
    @SerializedName("is_finished")
    @Expose
    var isFinised: Boolean = false
) : Parcelable

@Parcelize
data class SignInRequest(
    @SerializedName("name")
    @Expose
    var name: String,
    @SerializedName("password")
    @Expose
    var password: String,
    @SerializedName("remember")
    @Expose
    var remember: Boolean = false
) : Parcelable
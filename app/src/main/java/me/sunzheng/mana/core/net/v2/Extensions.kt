package me.sunzheng.mana.core.net.v2

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.gson.Gson
import me.sunzheng.mana.core.net.v2.database.EpisodeEntity
import me.sunzheng.mana.core.net.v2.database.VideoFileEntity
import me.sunzheng.mana.core.net.v2.database.WatchProgressEntity
import java.util.UUID

fun Activity.showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Activity.showToast(@StringRes res: Int) {
    Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
}

fun Fragment.showToast(msg: String) {
    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}

fun Fragment.showToast(@StringRes res: Int) {
    Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show()
}

fun VideoFileEntity.parseExtractorMediaSource(host: String, dataSourceFactory: DataSource.Factory) =
    url?.toUri()?.let {
        ExtractorMediaSource.Factory(dataSourceFactory)
            .createMediaSource("$host${it}".toUri())
    }

fun EpisodeEntity.parseMediaDescription(mediaUrl: String) =
    MediaDescriptionCompat.Builder()
        .setMediaId(this.id.toString())
        .setTitle(this.nameCn)
        .setSubtitle(this.name)
        .setDescription("")
        .setIconUri(this.thumbnailImage?.url?.toUri() ?: null)
        .setIconBitmap(null)
        .setMediaUri(mediaUrl.toUri())
        .setExtras(Bundle().apply {
            putParcelable("raw", this@parseMediaDescription)
            putString("bangumiId", this@parseMediaDescription.bangumiId?.toString() ?: "")
        })
        .build()

fun String.toUUID() = UUID.fromString(this)

fun WatchProgressEntity.parseRecord() = Gson().fromJson(Gson().toJson(this), Record::class.java)
fun Record.parseWatchProgressEntity() =
    Gson().fromJson(Gson().toJson(this), WatchProgressEntity::class.java)

fun ImageView.loadUrl(url: String, domainColor: String? = null) {
    var m = domainColor?.let { RequestOptions().placeholder(ColorDrawable(Color.parseColor(it))) }

    var r = Glide.with(this)
        .load(url)
        .downsample(DownsampleStrategy.CENTER_INSIDE)
        .transition(DrawableTransitionOptions.withCrossFade())
    m?.run {
        r.apply(this)
    }
    r.into(this)
}
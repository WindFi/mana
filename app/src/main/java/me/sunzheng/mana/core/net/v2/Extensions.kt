package me.sunzheng.mana.core.net.v2

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.core.util.PatternsCompat
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
//import com.google.android.exoplayer2.source.ExtractorMediaSource
//import com.google.android.exoplayer2.source.ProgressiveMediaSource
//import com.google.android.exoplayer2.upstream.DataSource
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

@UnstableApi
fun VideoFileEntity.parseExtractorMediaSource(host: String, dataSourceFactory: DataSource.Factory) =
    url?.toUri()?.let {
        var url = if (PatternsCompat.WEB_URL.matcher(it.toString()).find()) {
            it.toString()
        } else {
            "$host${it}"
        }
        ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource("$url".toUri().parseMediaItem())
    }
fun VideoFileEntity.parseMediaItem(host: String, dataSourceFactory: DataSource.Factory) =
    url?.toUri()?.let {
        var url = if (PatternsCompat.WEB_URL.matcher(it.toString()).find()) {
            it.toString()
        } else {
            "$host${it}"
        }
        url.toUri().parseMediaItem()
    }
fun Uri.parseMediaItem() = MediaItem.fromUri(this)

/**
 * Creates a MediaItem with metadata including Bangumi cover artwork.
 * This allows MediaSession to display the Bangumi cover in the notification.
 */
fun Uri.parseMediaItemWithMetadata(
    title: String? = null,
    artist: String? = null,
    artworkUri: Uri? = null
): MediaItem {
    val builder = MediaItem.Builder()
        .setUri(this)
    
    if (title != null || artist != null || artworkUri != null) {
        val metadataBuilder = androidx.media3.common.MediaMetadata.Builder()
        title?.let { metadataBuilder.setTitle(it) }
        artist?.let { metadataBuilder.setArtist(it) }
        artworkUri?.let { metadataBuilder.setArtworkUri(it) }
        builder.setMediaMetadata(metadataBuilder.build())
    }
    
    return builder.build()
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
    var m = domainColor?.let {
        RequestOptions().placeholder(
            ColorDrawable(
                try {
                    Color.parseColor(it)
                } catch (e: Exception) {
                    Color.parseColor("#000000")
                }
            )
        )
    }

    var r = Glide.with(this)
        .load(url)
        .downsample(DownsampleStrategy.CENTER_INSIDE)
        .transition(DrawableTransitionOptions.withCrossFade())
    m?.run {
        r.apply(this)
    }
    r.into(this)
}
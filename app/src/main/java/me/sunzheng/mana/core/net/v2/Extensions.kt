package me.sunzheng.mana.core.net.v2

import android.app.Activity
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import me.sunzheng.mana.core.net.v2.database.VideoFileEntity

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

fun VideoFileEntity.parseExtractorMediaSource(dataSourceFactory: DataSource.Factory) =
    url?.toUri()?.let {
        ExtractorMediaSource.Factory(dataSourceFactory)
            .createMediaSource("https://suki.moe${it}".toUri())
    }
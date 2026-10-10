package me.sunzheng.mana.utils

import android.net.Uri
import android.text.TextUtils
import androidx.core.net.toUri
import kotlin.math.roundToInt

object HostUtil {

    const val IMAGEKIT = "https://ik.imagekit.io"

    @JvmStatic
    fun makeUp(host: String? = null, url: String? = null): String {
        var _host = if (host!!.endsWith("/"))
            host.substring(0, host.length - 1)
        else host
        var uri = Uri.parse(url)
        return if (!TextUtils.isEmpty(uri.host)) url!! else {
//            host + url
            (_host + url).toUri().toString()
        }
    }

    /**
     * Appends an ImageKit transformation (`tr=w-..,h-..`) to [url] when it is hosted on ImageKit.
     *
     * The returned url is used as the Glide model, so it also becomes the cache key: the same
     * asset requested at two different sizes is cached (and transformed by ImageKit) separately.
     *
     * @param width  target width in px, <= 0 when unknown.
     * @param height target height in px, <= 0 when unknown.
     * @param ratio  target height / width, <= 0 when unknown. Only used to derive the missing
     *               dimension when exactly one of [width] / [height] is known.
     * @return the transformed url, or [url] unchanged when it is not an ImageKit url or when no
     *         dimension can be derived.
     */
    @JvmStatic
    fun imagekitSupport(
        url: String,
        width: Float = -1f,
        height: Float = -1f,
        ratio: Float = -1f
    ): String {
        if (!url.startsWith(IMAGEKIT)) return url

        val widthPx = dimension(width)
        val heightPx = dimension(height)
        val endFix: String? = when {
            widthPx != null && heightPx != null -> "w-$widthPx,h-$heightPx"
            widthPx != null -> "w-$widthPx" + (dimension(width * ratio)?.let { ",h-$it" } ?: "")
            heightPx != null -> "h-$heightPx" + (dimension(height / ratio)?.let { ",w-$it" } ?: "")
            else -> null
        } ?: return url

        // Respect an explicit transformation already present in the url.
        if (url.hasTransformParam()) return url

        val separator = when {
            url.endsWith("?") || url.endsWith("&") -> ""
            url.contains('?') -> "&"
            else -> "?"
        }
        return "$url${separator}tr=$endFix"
    }

    /**
     * Rounds a positive dimension to the nearest multiple of 20 so ImageKit can reuse its own
     * cache across slightly different view sizes.
     *
     * @return the rounded value, or null when [value] is not usable.
     */
    private fun dimension(value: Float): Int? {
        if (value <= 0f || !value.isFinite()) return null
        val rounded = (value / 20f).roundToInt() * 20
        return if (rounded > 0) rounded else null
    }

    /** Returns true when the query string already declares a `tr` parameter. */
    private fun String.hasTransformParam(): Boolean =
        substringAfter('?', "").split('&').any { it.substringBefore('=') == "tr" }
}
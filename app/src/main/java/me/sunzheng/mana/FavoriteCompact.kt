package me.sunzheng.mana

import android.annotation.TargetApi
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Created by Sun on 2017/7/6.
 */
object FavoriteCompact {
    var colors = intArrayOf(
        android.R.attr.buttonStyle,
        R.color.favorite_wish,
        R.color.favorite_watched,
        R.color.favorite_watching,
        R.color.favorite_pause,
        R.color.favorite_abanoned
    )

    fun setFavorite(status: Int, v: TextView) {
        val statusString =
            v.resources.getStringArray(R.array.favorite_status_values)[status.toInt()]
        var colorInt = when (status) {
            1 -> R.color.favorite_wish
            2 -> R.color.favorite_watched
            3 -> R.color.favorite_watching
            4 -> R.color.favorite_pause
            5 -> R.color.favorite_abanoned
            else -> R.color.favorite_abanoned
        }
        v.setBackgroundColor(ContextCompat.getColor(v.context, colorInt))
        v.text = statusString
    }

    internal abstract class DrawableCompat(val context: Context) {
        abstract fun getDrawable(status: Long, v: TextView): Drawable?
    }

    internal class BaseDrawableCompatImpl(mContext: Context) : DrawableCompat(mContext) {
        override fun getDrawable(status: Long, v: TextView): Drawable? {
            return null
        }
    }

    @TargetApi(21)
    internal class LolipopDrawableCompatImpl(mContext: Context) : DrawableCompat(mContext) {
        override fun getDrawable(status: Long, v: TextView): Drawable? {
            val cstatus = arrayOf(intArrayOf(android.R.attr.state_pressed))
            return RippleDrawable(
                ColorStateList(cstatus, colors),
                ColorDrawable(
                    ContextCompat.getColor(
                        v.context,
                        colors[status.toInt() % colors.size]
                    )
                ),
                v.background
            )
        }
    }
}
package me.sunzheng.mana.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

object ViewAdapters {
    @JvmStatic
    @BindingAdapter("url", "place_holder", requireAll = false)
    fun appUrl(view: ImageView, url: String? = null, placeholder: String? = null) {
        url?.run {
            val host = view.context.getSharedPreferences(
                PreferenceManager.Global.STR_SP_NAME,
                Context.MODE_PRIVATE
            ).getString(PreferenceManager.Global.STR_KEY_HOST, "")
            var placeHolderDrawable =
                placeholder?.let { ColorDrawable(Color.parseColor(placeholder)) }
            var requestBuilder = Glide.with(view)
                .load(HostUtil.makeUp(host, url))
            placeHolderDrawable?.run {
                requestBuilder.placeholder(this)
            }
            requestBuilder.into(view)
        }
    }
}
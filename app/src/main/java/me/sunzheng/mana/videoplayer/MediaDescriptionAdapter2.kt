package me.sunzheng.mana.videoplayer

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.media.MediaDescriptionCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import me.sunzheng.mana.R

class MediaDescriptionAdapter2(
    val context: Context,
    var list: List<MediaDescriptionCompat>? = null
) : BaseAdapter() {

    override fun getCount(): Int = list?.size ?: 0
    override fun getItem(position: Int): MediaDescriptionCompat? = list?.get(position)

    override fun getItemId(position: Int): Long = 0

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var v = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_video_list, null
        )
        var viewHolder = v.tag ?: ViewHolder(v)
        v.tag = viewHolder
        var item = list?.get(position) ?: null
        item?.run {
            if (viewHolder is ViewHolder) {
                viewHolder.imageView.alpha = 0.3f
                Glide.with(v).load(iconUri)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            p0: GlideException?,
                            p1: Any?,
                            p2: Target<Drawable>?,
                            p3: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            p0: Drawable?,
                            p1: Any?,
                            p2: Target<Drawable>?,
                            p3: DataSource?,
                            p4: Boolean
                        ): Boolean {
                            viewHolder.imageView.alpha = 1f
                            return false
                        }

                    }).into(viewHolder.imageView)
                viewHolder.titleTextView.text = item.title
                viewHolder.seqTextView.text = "${count - position}"
            }
        }
        return v
    }
}

internal class ViewHolder(rootView: View) {
    var imageView: ImageView
    var titleTextView: TextView
    var seqTextView: TextView

    init {
        imageView = rootView.findViewById(R.id.imageView)
        titleTextView = rootView.findViewById(R.id.title)
        seqTextView = rootView.findViewById(R.id.episode_seq_textview)
    }
}
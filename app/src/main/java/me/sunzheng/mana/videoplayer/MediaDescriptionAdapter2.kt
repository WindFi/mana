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
import me.sunzheng.mana.core.net.v2.database.EpisodeEntity
import java.util.Collections

abstract class SortListAdapter<T>(val list: MutableList<T> = mutableListOf()) : BaseAdapter() {
    override fun getCount(): Int = list.size
    override fun getItem(position: Int): T? = list[position]

    override fun getItemId(position: Int): Long = 0

    fun addAll(items: Collection<T>) {
        internalAddAll(items).run {
            list.clear()
            list.addAll(this)
        }

        internalSort()

        notifyDataSetChanged()
    }

    private fun internalAddAll(items: Collection<T>): List<T> {
        val copy = MutableList(size = list.size) { list[it] }
        Collections.copy(copy, list)
        items.forEach { source ->
            if (list.filter { !sameTo(source, it) }.isEmpty()) {
                copy.add(source)
            }
        }
        return copy
    }

    private fun internalSort() {
        for (i in 0 until list.size - 1) {
            for (j in i + 1 until list.size) {
                if (compareTo(list[i], list[j]) > 0) {
                    val value = list[i]
                    list[i] = list[j]
                    list[j] = value
                }
            }
        }
    }

    abstract fun sameTo(old: T, new: T): Boolean
    abstract fun compareTo(old: T, new: T): Int
}

class MediaDescriptionAdapter2(
    val context: Context,
    mList: List<MediaDescriptionCompat>
) : SortListAdapter<MediaDescriptionCompat>(mList.toMutableList()) {
    override fun sameTo(old: MediaDescriptionCompat, new: MediaDescriptionCompat): Boolean =
        old.mediaId == new.mediaId

    override fun compareTo(old: MediaDescriptionCompat, new: MediaDescriptionCompat): Int {
        val oldSort = old.extras?.getParcelable<EpisodeEntity>("raw")
        val newSort = new.extras?.getParcelable<EpisodeEntity>("raw")
        if (oldSort == null)
            return -1
        if (newSort == null)
            return 1
        return oldSort.episodeNo - newSort.episodeNo
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val v = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_video_list, null
        )
        val viewHolder = v.tag ?: ViewHolder(v)
        v.tag = viewHolder
        val item = list.get(position)
        item.run {
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
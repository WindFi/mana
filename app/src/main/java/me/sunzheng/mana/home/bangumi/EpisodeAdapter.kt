package me.sunzheng.mana.home.bangumi

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.runBlocking
import me.sunzheng.mana.R
import me.sunzheng.mana.core.net.v2.database.EpisodeAndWatchprogress
import me.sunzheng.mana.core.net.v2.database.EpisodeEntity
import me.sunzheng.mana.core.net.v2.loadUrl
import me.sunzheng.mana.utils.HostUtil.makeUp
import me.sunzheng.mana.utils.LanguageSwitchUtils
import me.sunzheng.mana.utils.PreferenceManager

/**
 * Created by Sun on 2017/7/14.
 */
class EpisodeAdapter(var onItemClickListener: ((View, Int, Long, EpisodeEntity) -> Unit)? = null) :
    ListAdapter<EpisodeAndWatchprogress, EpisodeAdapter.ViewHolder>(object :
        DiffUtil.ItemCallback<EpisodeAndWatchprogress>() {
        override fun areItemsTheSame(
            oldItem: EpisodeAndWatchprogress,
            newItem: EpisodeAndWatchprogress
        ): Boolean =
            oldItem.episodeEntity.id == newItem.episodeEntity.id

        override fun areContentsTheSame(
            oldItem: EpisodeAndWatchprogress,
            newItem: EpisodeAndWatchprogress
        ): Boolean =
            oldItem.hashCode() == newItem.hashCode()

        override fun getChangePayload(
            oldItem: EpisodeAndWatchprogress,
            newItem: EpisodeAndWatchprogress
        ): Any = newItem
    }) {
    var host: String? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bangumidetails_fragment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        getItem(position).run {
            episodeEntity.run {
                if (TextUtils.isEmpty(host)) {
                    host = holder.itemView.context.getSharedPreferences(
                        PreferenceManager.Global.STR_SP_NAME,
                        Context.MODE_PRIVATE
                    )
                        .getString(PreferenceManager.Global.STR_KEY_HOST, "")
                }
                holder.mTitleTextView.text = nameCn
                if (status == 2L) {
                    holder.itemView.setOnClickListener { v ->
                        onItemClickListener?.invoke(v, position, getItemId(position), this)
                    }
                    holder.itemView.isClickable = true
                } else {
                    holder.itemView.isClickable = false
                }
                val coverImage = thumbnailImage
                if (coverImage != null) {
                    coverImage.url = makeUp(host, coverImage.url)
                }
                var coverColor = thumbnailImage?.dominantColor ?: thumbnailColor
                holder.mImageView.loadUrl(coverImage!!.url, coverColor)
                holder.mEpisodeNoTextView.text = holder.itemView.context.getString(
                    R.string.episode_template,
                    episodeNo.toString() + ""
                )
                holder.mTitleTextView.text =
                    LanguageSwitchUtils.switchLanguageToJa(holder.itemView.context, name, nameCn)
                holder.mUpdateDateTextView.text = airdate
            }

            // watchProgress
            watchProgress?.run {
                holder.mProgressBar.visibility = View.VISIBLE
                holder.mProgressBar.max = 100
                holder.mProgressBar.progress =
                    if (percentage * 100 < 1) 1 else (percentage * 100.0).toInt()
            } ?: runBlocking {
                holder.mProgressBar.visibility = View.GONE
            }
        }

    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mTitleTextView: TextView
        val mUpdateDateTextView: TextView
        val mEpisodeNoTextView: TextView
        val mImageView: ImageView
        val mProgressBar: ProgressBar

        init {
            mEpisodeNoTextView = view.findViewById(R.id.item_title_textview)
            mTitleTextView = view.findViewById(R.id.item_subtitle_textview)
            mUpdateDateTextView = view.findViewById(R.id.item_etc_textview)
            mImageView = view.findViewById(R.id.item_album)
            mProgressBar = view.findViewById(R.id.item_progressbar)
        }
    }
}

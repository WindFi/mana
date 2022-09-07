package me.sunzheng.mana.home.bangumi

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
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
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.RequestOptions
import me.sunzheng.mana.R
import me.sunzheng.mana.core.net.v2.database.EpisodeEntity
import me.sunzheng.mana.utils.HostUtil.makeUp
import me.sunzheng.mana.utils.LanguageSwitchUtils
import me.sunzheng.mana.utils.PreferenceManager

/**
 * Created by Sun on 2017/7/14.
 */
class EpisodeAdapter(var onItemClickListener: ((View, Int, Long, EpisodeEntity) -> Unit)? = null) :
    ListAdapter<EpisodeEntity, EpisodeAdapter.ViewHolder>(object :
        DiffUtil.ItemCallback<EpisodeEntity>() {
        override fun areItemsTheSame(oldItem: EpisodeEntity, newItem: EpisodeEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: EpisodeEntity, newItem: EpisodeEntity): Boolean =
            oldItem.hashCode() == newItem.hashCode()

        override fun getChangePayload(oldItem: EpisodeEntity, newItem: EpisodeEntity): Any = newItem
    }) {
    var mHandler = Handler(Looper.getMainLooper())
    var host: String? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bangumidetails_fragment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        if (TextUtils.isEmpty(host)) {
            host = holder.itemView.context.getSharedPreferences(
                PreferenceManager.Global.STR_SP_NAME,
                Context.MODE_PRIVATE
            )
                .getString(PreferenceManager.Global.STR_KEY_HOST, "")
        }
        holder.mTitleTextView.text = item.nameCn
        if (item.status == 2L) {
            holder.itemView.setOnClickListener { v ->
//                val context = v.context
//                mHandler.postDelayed({
//                    val intent =
//                        VideoPlayActivity.newInstance(
//                            context,
//                            itemCount - (position + 1),
//                            currentList
//                        )
//                    context.startActivity(intent)
//                }, 300)
                onItemClickListener?.invoke(v, position, getItemId(position), item)
            }
            holder.itemView.isClickable = true
        } else {
            holder.itemView.isClickable = false
        }
        val coverImage = item.thumbnailImage
        if (coverImage != null) {
            coverImage.url = makeUp(host, coverImage.url)
        }
//        item.thumbnail = makeUp(host, item.thumbnail)
        val request: RequestBuilder<*> = Glide.with(holder.itemView.context)
            .load(coverImage?.url ?: "")

        var options = RequestOptions()
        item.thumbnailImage?.dominantColor?.run {
            options.placeholder(ColorDrawable(Color.parseColor(item.thumbnailImage.dominantColor)))
        }
        request.apply(options)
        request.into(holder.mImageView)
        holder.mEpisodeNoTextView.text = holder.itemView.context.getString(
            R.string.episode_template,
            item.episodeNo.toString() + ""
        )
        holder.mTitleTextView.text =
            LanguageSwitchUtils.switchLanguageToJa(holder.itemView.context, item.name, item.nameCn)
        //        holder.mTitleTextView.setText(TextUtils.isEmpty(item.getNameCn()) ? item.getName() : item.getNameCn());
        holder.mUpdateDateTextView.text = item.airdate
        // TODO: 2021/12/5  watchProgress
//        val watchProgress = item.watchProgress
//        if (watchProgress == null) {
//            holder.mProgressBar.visibility = View.GONE
//        } else {
//            holder.mProgressBar.visibility = View.VISIBLE
//            holder.mProgressBar.max = 100
//            holder.mProgressBar.progress =
//                if (item.watchProgress.percentage * 100 < 1) 1 else (item.watchProgress.percentage * 100.0).toInt()
//        }
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

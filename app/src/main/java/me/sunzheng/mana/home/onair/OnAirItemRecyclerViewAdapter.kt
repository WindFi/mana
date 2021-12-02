package me.sunzheng.mana.home.onair

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import me.sunzheng.mana.R
import me.sunzheng.mana.core.net.v2.database.BangumiEntity
import me.sunzheng.mana.databinding.ItemOnairfragmentBinding
import me.sunzheng.mana.utils.ArrarysResourceUtils
import me.sunzheng.mana.utils.HostUtil
import me.sunzheng.mana.utils.LanguageSwitchUtils
import me.sunzheng.mana.utils.PreferenceManager

class OnAirItemRecyclerViewAdapter(
    items: List<BangumiEntity>,
    var onItemClickListener: ((View, Int, Long, Any) -> Unit)? = null
) :
    RecyclerView.Adapter<OnAirItemRecyclerViewAdapter.ViewHolder>() {
    private val mValues: SortedList<BangumiEntity> = SortedList(
        BangumiEntity::class.java,
        object : SortedListAdapterCallback<BangumiEntity?>(this) {
            override fun compare(o1: BangumiEntity?, o2: BangumiEntity?): Int = 0

            override fun areContentsTheSame(
                oldItem: BangumiEntity?,
                newItem: BangumiEntity?
            ): Boolean = oldItem?.id == newItem?.id

            override fun areItemsTheSame(item1: BangumiEntity?, item2: BangumiEntity?): Boolean =
                item1?.id == item2?.id

        }
    )

    init {
        mValues.addAll((items))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemOnairfragmentBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.root.setOnClickListener {
            onItemClickListener?.invoke(it, position, 0, mValues[position])
        }
        val host = holder.itemView.context.getSharedPreferences(
            PreferenceManager.Global.STR_SP_NAME,
            Context.MODE_PRIVATE
        )
            .getString(PreferenceManager.Global.STR_KEY_HOST, "")
        val coverImage = mValues[position].coverImage
        val domainColor =
            if (coverImage == null) mValues[position].cover_color else coverImage.dominantColor
        var coverImageUrl = if (coverImage == null) mValues[position].image else coverImage.url
        coverImageUrl = HostUtil.makeUp(host, coverImageUrl)
        Glide.with(holder.itemView.context)
            .load(coverImageUrl)
            .apply(RequestOptions().placeholder(ColorDrawable(Color.parseColor(domainColor))))
            .into(holder.binding.itemAlbum)
        holder.binding.itemTitleTextview.text = LanguageSwitchUtils.switchLanguageToJa(
            holder.binding.root.context,
            mValues.get(position).name,
            mValues.get(position).nameCn
        ).trim()
        holder.binding.itemSubtitleTextview.text = mValues.get(position).summary
        val dayInWeek = ArrarysResourceUtils.dayInWeek(
            holder.itemView.context, mValues[position].airWeekday
                .toInt()
        )
        val resultString = holder.itemView.context.getString(
            R.string.formatter_day_airdate,
            mValues[position].airDate,
            dayInWeek
        )
        holder.binding.itemEtcTextview.text = resultString
    }

    override fun getItemCount(): Int {
        return mValues.size()
    }

    class ViewHolder(var binding: ItemOnairfragmentBinding) : RecyclerView.ViewHolder(binding.root)
}
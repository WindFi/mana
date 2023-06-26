package me.sunzheng.mana.home.onair

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import me.sunzheng.mana.R
import me.sunzheng.mana.core.net.v2.database.BangumiEntity
import me.sunzheng.mana.core.net.v2.loadUrl
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
    val mValues: SortedList<BangumiEntity> = SortedList(
        BangumiEntity::class.java,
        object : SortedListAdapterCallback<BangumiEntity?>(this) {
            override fun compare(o1: BangumiEntity?, o2: BangumiEntity?): Int = 0

            override fun areContentsTheSame(
                oldItem: BangumiEntity?,
                newItem: BangumiEntity?
            ): Boolean = oldItem?.id == newItem?.id

            override fun areItemsTheSame(item1: BangumiEntity?, item2: BangumiEntity?): Boolean =
                item1?.hashCode() == item2?.hashCode()
        }
    )

    init {
        mValues.addAll((items))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemOnairfragmentBinding.inflate(LayoutInflater.from(parent.context)))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        mValues[position].run {
            holder.binding.root.setOnClickListener {
                onItemClickListener?.invoke(it, position, 0, this)
            }
            val host = holder.itemView.context.getSharedPreferences(
                PreferenceManager.Global.STR_SP_NAME,
                Context.MODE_PRIVATE
            ).getString(PreferenceManager.Global.STR_KEY_HOST, "")
            val domainColor = coverImage?.dominantColor ?: cover_color!!
            var coverImageUrl = coverImage?.url ?: image!!

            coverImageUrl = HostUtil.makeUp(host, coverImageUrl)

            holder.binding.itemAlbum.loadUrl(coverImageUrl, domainColor)

            holder.binding.itemTitleTextview.text = LanguageSwitchUtils.switchLanguageToJa(
                holder.binding.root.context,
                name,
                nameCn
            ).trim()
            holder.binding.itemSubtitleTextview.text = summary
            val dayInWeek = ArrarysResourceUtils.dayInWeek(
                holder.itemView.context, airWeekday
                    .toInt()
            )
            val resultString = holder.itemView.context.getString(
                R.string.formatter_day_airdate,
                airDate,
                dayInWeek
            )
            holder.binding.itemEtcTextview.text = resultString
        }
    }

    override fun getItemCount(): Int {
        return mValues.size()
    }

    class ViewHolder(var binding: ItemOnairfragmentBinding) : RecyclerView.ViewHolder(binding.root)
}
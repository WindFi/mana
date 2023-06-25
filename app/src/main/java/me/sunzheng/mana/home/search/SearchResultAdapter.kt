package me.sunzheng.mana.home.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.SortedListAdapterCallback
import me.sunzheng.mana.R
import me.sunzheng.mana.core.net.v2.database.BangumiEntity
import me.sunzheng.mana.core.net.v2.loadUrl
import me.sunzheng.mana.utils.ArrarysResourceUtils
import me.sunzheng.mana.utils.LanguageSwitchUtils

/**
 * Created by Sun on 2017/6/20.
 */
class SearchResultAdapter(
    var values: List<BangumiEntity>,
    var onItemClickListener: ((view: View, position: Int, id: Long, model: Any) -> Unit)? = null
) : RecyclerView.Adapter<ViewHolder>() {
    val mValues: SortedList<BangumiEntity> = SortedList(
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
        mValues.addAll(values)
    }

    override fun getItemCount(): Int {
        return mValues.size()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onairfragment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(it, position, 0, mValues[position])
//            mValues.run {
//                BangumiDetailsActivity.newInstance(
//                    it.context as Activity,
//                    mValues[position],
//                    holder.mImageView
//                )
//            }
        }
//        Glide.with(holder.itemView.context)
//            .load(mValues[position].image)
//            .apply(
//                RequestOptions()
//                    .placeholder(ColorDrawable(Color.parseColor(mValues[position].cover_color)))
//            )
//            .into(holder.mImageView)
        mValues[position].image?.run {
            holder.mImageView.loadUrl(this, mValues[position].cover_color)
        }

        holder.mTitleTextView.text = LanguageSwitchUtils.switchLanguageToJa(
            holder.itemView.context,
            mValues[position].name,
            mValues[position].nameCn
        )
        holder.mSummaryTextView.text = mValues[position].summary
        val dayInWeek = ArrarysResourceUtils.dayInWeek(
            holder.itemView.context,
            mValues[position].airWeekday.toInt()
        )
        val resultString = holder.itemView.context.getString(
            R.string.formatter_day_airdate,
            mValues[position].airDate,
            dayInWeek
        )
        //        int color= ContextCompat.getColor(holder.itemView.getContext(),R.color.colorPrimary);
//        SpannableStringBuilder spannableStringBuilder=new SpannableStringBuilder(resultString);
//        spannableStringBuilder.setSpan(new ForegroundColorSpan(color),resultString.indexOf(dayInWeek),resultString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.mEtcTextView.text = resultString
    }
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val mView: CardView
    val mTitleTextView: TextView
    val mSummaryTextView: TextView
    val mEtcTextView: TextView
    val mImageView: ImageView

    init {
        mView = view as CardView
        mTitleTextView = view.findViewById(R.id.item_title_textview)
        mImageView = view.findViewById(R.id.item_album)
        mSummaryTextView = view.findViewById(R.id.item_subtitle_textview)
        mEtcTextView = view.findViewById(R.id.item_etc_textview)
    }
}
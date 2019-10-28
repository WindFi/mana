package me.sunzheng.mana.home.onair;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.TransformationUtils;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.util.pool.GlideTrace;

import java.util.List;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;
import androidx.recyclerview.widget.SortedListAdapterCallback;

import me.sunzheng.mana.BangumiDetailsActivity;
import me.sunzheng.mana.R;
import me.sunzheng.mana.core.BangumiModel;
import me.sunzheng.mana.core.CoverImage;
import me.sunzheng.mana.utils.ArrarysResourceUtils;
import me.sunzheng.mana.utils.HostUtil;
import me.sunzheng.mana.utils.LanguageSwitchUtils;
import me.sunzheng.mana.utils.PreferenceManager;

public class OnAirItemRecyclerViewAdapter extends RecyclerView.Adapter<OnAirItemRecyclerViewAdapter.ViewHolder> {

    private final SortedList<BangumiModel> mValues;

    public OnAirItemRecyclerViewAdapter(List<BangumiModel> items) {
        mValues = new SortedList<BangumiModel>(BangumiModel.class, new SortedListAdapterCallback<BangumiModel>(this) {
            @Override
            public int compare(BangumiModel o1, BangumiModel o2) {
                return 0;
            }

            @Override
            public boolean areContentsTheSame(BangumiModel oldItem, BangumiModel newItem) {
                return oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areItemsTheSame(BangumiModel item1, BangumiModel item2) {
                return item1.getId().equals(item2.getId());
            }
        });
        mValues.addAll(items);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onairfragment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BangumiDetailsActivity.newInstance((Activity) v.getContext(), mValues.get(position).getId().toString(), mValues.get(position).getImage(), LanguageSwitchUtils.switchLanguageToJa(holder.mView.getContext(), mValues.get(position).getName(), mValues.get(position).getNameCn()),
                        holder.mImageView);
            }
        });
        String host = holder.itemView.getContext().getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE)
                .getString(me.sunzheng.mana.utils.PreferenceManager.Global.STR_KEY_HOST, "");
        CoverImage coverImage = mValues.get(position).getCoverImage();
        String domainColor = coverImage == null ? mValues.get(position).getCover_color() : coverImage.dominantColor;
        String coverImageUrl = coverImage == null ? mValues.get(position).getImage() : coverImage.url;
        coverImageUrl = HostUtil.makeUp(host, coverImageUrl);

        Glide.with(holder.itemView.getContext())
                .load(coverImageUrl)
                .apply(new RequestOptions().placeholder(new ColorDrawable(Color.parseColor(domainColor))))
                .into(holder.mImageView);
        holder.mTitleTextView.setText(LanguageSwitchUtils.switchLanguageToJa(holder.mView.getContext(), mValues.get(position).getName(), mValues.get(position).getNameCn()));
        holder.mSummaryTextView.setText(mValues.get(position).getSummary());

        String dayInWeek = ArrarysResourceUtils.dayInWeek(holder.itemView.getContext(), (int) mValues.get(position).getAirWeekday());
        String resultString = holder.itemView.getContext().getString(R.string.formatter_day_airdate, mValues.get(position).getAirDate(), dayInWeek);
//        int color= ContextCompat.getColor(holder.itemView.getContext(),R.color.colorPrimary);
//        SpannableStringBuilder spannableStringBuilder=new SpannableStringBuilder(resultString);
//        spannableStringBuilder.setSpan(new ForegroundColorSpan(color),resultString.indexOf(dayInWeek),resultString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.mEtcTextView.setText(resultString);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public final CardView mView;
        public final TextView mTitleTextView, mSummaryTextView, mEtcTextView;
        public final ImageView mImageView;

        public ViewHolder(View view) {
            super(view);
            mView = (CardView) view;
            mTitleTextView = (TextView) view.findViewById(R.id.item_title_textview);
            mImageView = (ImageView) view.findViewById(R.id.item_album);
            mSummaryTextView = (TextView) view.findViewById(R.id.item_subtitle_textview);
            mEtcTextView = (TextView) view.findViewById(R.id.item_etc_textview);
        }
    }
}

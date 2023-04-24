package me.sunzheng.mana.home.search;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import me.sunzheng.mana.R;
import me.sunzheng.mana.core.BangumiModel;
import me.sunzheng.mana.utils.ArrarysResourceUtils;
import me.sunzheng.mana.utils.LanguageSwitchUtils;

/**
 * Created by Sun on 2017/6/20.
 */

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    List<BangumiModel> mValues;

    public SearchResultAdapter(List<BangumiModel> list) {
        this.mValues = list;
    }

    @Override
    public int getItemCount() {
        return mValues.size();
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
                // TODO: 2021/12/7 implement it
//                BangumiDetailsActivity.newInstance((Activity) v.getContext(), mValues.get(position), holder.mImageView);
            }
        });
        Glide.with(holder.itemView.getContext())
                .load(mValues.get(position).getImage())
                .apply(new RequestOptions()
                        .placeholder(new ColorDrawable(Color.parseColor(mValues.get(position).getCover_color()))))
                .into(holder.mImageView);
        holder.mTitleTextView.setText(LanguageSwitchUtils.switchLanguageToJa(holder.itemView.getContext(), mValues.get(position).getName(), mValues.get(position).getNameCn()));
        holder.mSummaryTextView.setText(mValues.get(position).getSummary());
        String dayInWeek = ArrarysResourceUtils.dayInWeek(holder.itemView.getContext(), (int) mValues.get(position).getAirWeekday());
        String resultString = holder.itemView.getContext().getString(R.string.formatter_day_airdate, mValues.get(position).getAirDate(), dayInWeek);
//        int color= ContextCompat.getColor(holder.itemView.getContext(),R.color.colorPrimary);
//        SpannableStringBuilder spannableStringBuilder=new SpannableStringBuilder(resultString);
//        spannableStringBuilder.setSpan(new ForegroundColorSpan(color),resultString.indexOf(dayInWeek),resultString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        holder.mEtcTextView.setText(resultString);
    }

    protected final static class ViewHolder extends RecyclerView.ViewHolder {
        public final CardView mView;
        public final TextView mTitleTextView, mSummaryTextView, mEtcTextView;
        public final ImageView mImageView;

        public ViewHolder(View view) {
            super(view);
            mView = (CardView) view;
            mTitleTextView = view.findViewById(R.id.item_title_textview);
            mImageView = view.findViewById(R.id.item_album);
            mSummaryTextView = view.findViewById(R.id.item_subtitle_textview);
            mEtcTextView = view.findViewById(R.id.item_etc_textview);
        }
    }
}

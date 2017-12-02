package me.sunzheng.mana.home.onair;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.util.SortedList;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import me.sunzheng.mana.BangumiDetailsActivity;
import me.sunzheng.mana.R;
import me.sunzheng.mana.core.BangumiModel;

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
                BangumiDetailsActivity.newInstance((Activity) v.getContext(), mValues.get(position).getId().toString(), mValues.get(position).getImage(), mValues.get(position).getNameCn(),
                        holder.mImageView);
            }
        });
        Glide.with(holder.itemView.getContext())
                .load(mValues.get(position).getImage())
                .placeholder(new ColorDrawable(Color.parseColor(mValues.get(position).getCover_color())))
                .into(holder.mImageView);
        holder.mTitleTextView.setText(mValues.get(position).getNameCn());
        holder.mSummaryTextView.setText(mValues.get(position).getSummary());
        holder.mEtcTextView.setText(mValues.get(position).getAirDate());
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

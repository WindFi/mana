package me.sunzheng.mana.home.onair;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import me.sunzheng.mana.BangumiDetailsActivity;
import me.sunzheng.mana.R;
import me.sunzheng.mana.home.onair.wrapper.Datum;

public class OnAirItemRecyclerViewAdapter extends RecyclerView.Adapter<OnAirItemRecyclerViewAdapter.ViewHolder> {

    private final List<Datum> mValues;

    public OnAirItemRecyclerViewAdapter(List<Datum> items) {
        mValues = items;
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
                BangumiDetailsActivity.newInstance((Activity) v.getContext(), mValues.get(position).getId(), mValues.get(position).getImage(), mValues.get(position).getNameCn(), holder.mImageView);
            }
        });
        Glide.with(holder.itemView.getContext())
                .load(mValues.get(position).getImage())
                .placeholder(new ColorDrawable(Color.parseColor(mValues.get(position).getCover_color())))
                .into(holder.mImageView);
        holder.mIdView.setText(mValues.get(position).getNameCn());
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public final CardView mView;
        public final TextView mIdView;
        public final ImageView mImageView;

        public ViewHolder(View view) {
            super(view);
            mView = (CardView) view;
            mIdView = (TextView) view.findViewById(R.id.item_descript);
            mImageView = (ImageView) view.findViewById(R.id.item_album);
        }
    }
}

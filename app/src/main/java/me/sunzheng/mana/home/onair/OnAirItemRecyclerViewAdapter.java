package me.sunzheng.mana.home.onair;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
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
                Intent intent = new Intent(v.getContext(), BangumiDetailsActivity.class);
                Bundle extras = new Bundle();
                extras.putString(BangumiDetailsActivity.ARGS_ABLUM_URL_STR, mValues.get(position).getImage());
                extras.putString(BangumiDetailsActivity.ARGS_ID_STR,mValues.get(position).getId());
                intent.putExtras(extras);
                Pair<View, String> pair = Pair.create((View) holder.mImageView, "image");
                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) v.getContext(), pair);
                v.getContext().startActivity(intent, optionsCompat.toBundle());
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

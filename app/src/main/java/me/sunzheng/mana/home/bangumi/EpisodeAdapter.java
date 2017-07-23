package me.sunzheng.mana.home.bangumi;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import me.sunzheng.mana.R;
import me.sunzheng.mana.VideoPlayerActivity;
import me.sunzheng.mana.home.bangumi.wrapper.Episode;

/**
 * Created by Sun on 2017/7/14.
 */

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {
    List<Episode> values;

    public EpisodeAdapter(List<Episode> values) {
        this.values = values;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onairfragment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Episode item = values.get(position);
        holder.mTitleTextView.setText(item.getNameCn());
        if (item.getStatus() == 2L) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), VideoPlayerActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString(VideoPlayerActivity.ARGS_URI_STR, item.getId());
                    intent.putExtras(extras);
                    v.getContext().startActivity(intent);
                }
            });
            holder.itemView.setClickable(true);
        } else {
            holder.itemView.setClickable(false);
        }
        Glide.with(holder.itemView.getContext()).load(item.getThumbnail()).into(holder.mImageView);
        holder.mEpisodeNoTextView.setText(holder.itemView.getContext().getString(R.string.episode_template, item.getEpisodeNo() + ""));
        holder.mTitleTextView.setText(TextUtils.isEmpty(item.getNameCn()) ? item.getName() : item.getNameCn());
        holder.mUpdateDateTextView.setText(item.getAirdate());
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    protected static final class ViewHolder extends RecyclerView.ViewHolder {
        final View itemView;
        final TextView mTitleTextView, mUpdateDateTextView, mEpisodeNoTextView;
        final ImageView mImageView;

        public ViewHolder(View view) {
            super(view);
            itemView = view;
            mEpisodeNoTextView = (TextView) view.findViewById(R.id.item_title_textview);
            mTitleTextView = (TextView) view.findViewById(R.id.item_subtitle_textview);
            mUpdateDateTextView = (TextView) view.findViewById(R.id.item_etc_textview);
            mImageView = (ImageView) view.findViewById(R.id.item_album);
        }
    }
}

package me.sunzheng.mana.home.bangumi;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import me.sunzheng.mana.PreferenceManager;
import me.sunzheng.mana.R;
import me.sunzheng.mana.VideoPlayerActivity;
import me.sunzheng.mana.home.bangumi.wrapper.Episode;

/**
 * Created by Sun on 2017/5/29.
 */

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {
    List<Episode> mValue;

    public EpisodeAdapter(List<Episode> list) {
        this.mValue = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bangumidetails_fragment, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        SharedPreferences sp = holder.itemView.getContext().getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE);
        final String host = sp.getString(PreferenceManager.Global.STR_KEY_HOST, "");
        Glide.with(holder.itemView.getContext()).load(host + "/" + mValue.get(position).getThumbnail()).into(holder.mAblumImageView);
        holder.mEpisodeNoTextView.setText(mValue.get(position).getEpisodeNo() + "");
        holder.mTitleTextView.setText(mValue.get(position).getNameCn());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 2017/5/29  goto play activity
                Intent intent = new Intent(v.getContext(), VideoPlayerActivity.class);
                Bundle extras = new Bundle();
                String uri = host + "/play/" + mValue.get(position).getId();
                extras.putString(VideoPlayerActivity.ARGS_URI_STR, uri);
                intent.putExtras(extras);
                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mValue.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView mAblumImageView;
        TextView mEpisodeNoTextView, mTitleTextView, mAirDateTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            mAblumImageView = (ImageView) itemView.findViewById(R.id.ablum_imageview);
            mEpisodeNoTextView = (TextView) itemView.findViewById(R.id.episode_no_textview);
            mTitleTextView = (TextView) itemView.findViewById(R.id.episode_title_textview);
            mAirDateTextView = (TextView) itemView.findViewById(R.id.episode_airdate_textview);
        }
    }
}

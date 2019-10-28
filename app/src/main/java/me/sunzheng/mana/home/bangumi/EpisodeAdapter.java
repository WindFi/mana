package me.sunzheng.mana.home.bangumi;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import me.sunzheng.mana.R;
import me.sunzheng.mana.VideoPlayActivity;
import me.sunzheng.mana.core.CoverImage;
import me.sunzheng.mana.core.Episode;
import me.sunzheng.mana.core.WatchProgress;
import me.sunzheng.mana.utils.HostUtil;
import me.sunzheng.mana.utils.LanguageSwitchUtils;
import me.sunzheng.mana.utils.PreferenceManager;
import me.sunzheng.mana.utils.RegexUtils;

/**
 * Created by Sun on 2017/7/14.
 */

public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.ViewHolder> {
    Handler mHandler = new Handler();
    List<Episode> values;
    String host;

    public EpisodeAdapter(List<Episode> values) {
        this.values = values;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bangumidetails_fragment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final Episode item = values.get(position);
        if (TextUtils.isEmpty(host)) {
            host = holder.itemView.getContext().getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE)
                    .getString(PreferenceManager.Global.STR_KEY_HOST, "");
        }
        holder.mTitleTextView.setText(item.getNameCn());
        if (item.getStatus() == 2L) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Context context = v.getContext();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = VideoPlayActivity.newInstance(context, values.size() - (position + 1), values);
                            context.startActivity(intent);
                        }
                    }, 300);

                }
            });
            holder.itemView.setClickable(true);
        } else {
            holder.itemView.setClickable(false);
        }
        CoverImage coverImage = item.getThumbnailImage();
        if (coverImage != null) {
            coverImage.url = HostUtil.makeUp(host, coverImage.url);
        }
        item.setThumbnail(HostUtil.makeUp(host, item.getThumbnail()));
        RequestBuilder request = Glide.with(holder.itemView.getContext()).load(coverImage == null ? item.getThumbnail() : coverImage.url);
        RequestOptions options = null;
        if (item.getThumbnailImage() != null && !TextUtils.isEmpty(item.getThumbnailImage().dominantColor)
                && item.getThumbnailImage().dominantColor.matches(RegexUtils.ColorPattern)) {
            options = new RequestOptions();
            options.placeholder(new ColorDrawable(Color.parseColor(item.getThumbnailImage().dominantColor)));
        } else if (!TextUtils.isEmpty(item.getThumbnailColor()) && item.getThumbnailColor().matches(RegexUtils.ColorPattern)) {
            options = new RequestOptions();
            options.placeholder(new ColorDrawable(Color.parseColor(item.getThumbnailColor())));
        }
        if (options != null)
            request.apply(options);
        request.into(holder.mImageView);
        holder.mEpisodeNoTextView.setText(holder.itemView.getContext().getString(R.string.episode_template, item.getEpisodeNo() + ""));
        holder.mTitleTextView.setText(LanguageSwitchUtils.switchLanguageToJa(holder.itemView.getContext(), item.getName(), item.getNameCn()));
//        holder.mTitleTextView.setText(TextUtils.isEmpty(item.getNameCn()) ? item.getName() : item.getNameCn());
        holder.mUpdateDateTextView.setText(item.getAirdate());
        WatchProgress watchProgress = item.getWatchProgress();
        if (watchProgress == null) {
            holder.mProgressBar.setVisibility(View.GONE);
        } else {
            holder.mProgressBar.setVisibility(View.VISIBLE);
            holder.mProgressBar.setMax(100);
            holder.mProgressBar.setProgress(item.getWatchProgress().getPercentage() * 100 < 1 ? 1 : (int) (item.getWatchProgress().getPercentage() * 100.0));
        }
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    protected static final class ViewHolder extends RecyclerView.ViewHolder {
        final View itemView;
        final TextView mTitleTextView, mUpdateDateTextView, mEpisodeNoTextView;
        final ImageView mImageView;
        final ProgressBar mProgressBar;

        public ViewHolder(View view) {
            super(view);
            itemView = view;
            mEpisodeNoTextView = view.findViewById(R.id.item_title_textview);
            mTitleTextView = view.findViewById(R.id.item_subtitle_textview);
            mUpdateDateTextView = view.findViewById(R.id.item_etc_textview);
            mImageView = view.findViewById(R.id.item_album);
            mProgressBar = view.findViewById(R.id.item_progressbar);
        }
    }
}

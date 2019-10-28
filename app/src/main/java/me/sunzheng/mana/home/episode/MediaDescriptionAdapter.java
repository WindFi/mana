package me.sunzheng.mana.home.episode;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import me.sunzheng.mana.R;

/**
 * Created by Sun on 2018/1/9.
 */

public class MediaDescriptionAdapter extends BaseAdapter {
    Context mContext;
    DataRepository dataRepository;

    public MediaDescriptionAdapter(Context mContext, DataRepository dataRepository) {
        this.mContext = mContext;
        this.dataRepository = dataRepository;
    }

    @Override
    public int getCount() {
        return dataRepository.getEpisodeCount();
    }

    @Override
    public Object getItem(int position) {
        return dataRepository.getItemByPosition(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_video_list, null);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.imageView.setAlpha(0.3f);
        Glide.with(mContext).load(dataRepository.getItemByPosition(position).getIconUri()).listener(new RequestListener<Drawable>() {
            @Override
            public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                viewHolder.imageView.setAlpha(1.0f);
                return false;
            }
        }).into(viewHolder.imageView);
        viewHolder.titleTextView.setText(dataRepository.getItemByPosition(position).getDescription());
        viewHolder.seqTextView.setText(dataRepository.getItemByPosition(position).getTitle());
        return convertView;
    }

    static class ViewHolder {
        public View rootView;
        public ImageView imageView;
        public TextView titleTextView;
        public TextView seqTextView;

        public ViewHolder(View rootView) {
            this.rootView = rootView;
            imageView = rootView.findViewById(R.id.imageView);
            titleTextView = rootView.findViewById(R.id.title);
            seqTextView = rootView.findViewById(R.id.episode_seq_textview);
        }
    }
}

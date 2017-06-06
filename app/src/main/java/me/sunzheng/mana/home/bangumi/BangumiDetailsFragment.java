package me.sunzheng.mana.home.bangumi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import me.sunzheng.mana.BangumiDetailsActivity;
import me.sunzheng.mana.PreferenceManager;
import me.sunzheng.mana.R;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.bangumi.wrapper.Episode;

/**
 * Created by Sun on 2017/5/27.
 */

public class BangumiDetailsFragment extends Fragment implements HomeContract.Bangumi.View {

    ImageView mImageView;
    TextView mCNTitleTextView;
    TextView mOriginTitleTextView;
    TextView mSummaryTextView;
    TextView mAirDateTextView;
    TextView mWeekDayTextView;

    LinearLayoutCompat mEpisodeLinearLayout;

    HomeContract.Bangumi.Presenter mPresenter;
    SharedPreferences sharedPreferences;
    public BangumiDetailsFragment() {
    }

    public static BangumiDetailsFragment newInstance(Bundle extras) {
        BangumiDetailsFragment fragment = new BangumiDetailsFragment();
        fragment.setArguments(extras);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences=getActivity().getSharedPreferences(PreferenceManager.Global.STR_SP_NAME,Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public android.view.View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bangumidetails, null);
    }

    @Override
    public void onViewCreated(android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mImageView = (ImageView) view.findViewById(R.id.bangumidetails_ablum_imageview);
        mCNTitleTextView = (TextView) view.findViewById(R.id.bangumidetails_name_textview);
        mOriginTitleTextView = (TextView) view.findViewById(R.id.bangumidetails_originname_textview);
        mSummaryTextView = (TextView) view.findViewById(R.id.bangumidetails_summary_textview);
        mEpisodeLinearLayout = (LinearLayoutCompat) view.findViewById(R.id.bangumidetails_episode_linearlayout);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mImageView.setTransitionName(BangumiDetailsActivity.PAIR_IMAGE_STR);
        }
        Glide.with(this).load(getArguments().getString(BangumiDetailsActivity.ARGS_ABLUM_URL_STR)).into(mImageView);
        mPresenter.load(getArguments().getString(BangumiDetailsActivity.ARGS_ID_STR));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void setPresenter(HomeContract.Bangumi.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public void setWeekDay(long updateDate) {
        if (mWeekDayTextView == null)
            return;
        // TODO: 2017/5/27  will change to week from decimal
        mWeekDayTextView.setText(updateDate + "");
    }

    @Override
    public void setAirDate(String startDate) {
        if (mAirDateTextView == null)
            return;
        mAirDateTextView.setText(startDate);
    }

    @Override
    public void setSummary(CharSequence descript) {
        if (mSummaryTextView == null)
            return;
        mSummaryTextView.setText(descript);
    }

    @Override
    public void setOriginName(CharSequence originName) {
        if (mOriginTitleTextView == null)
            return;
        mOriginTitleTextView.setText(originName);
    }

    @Override
    public void setFaviorStatus(long status) {
        // TODO: 2017/5/27 favior status
    }

    @Override
    public void setName(CharSequence name) {
        if (mCNTitleTextView == null)
            return;
        mCNTitleTextView.setText(name);
    }

    @Override
    public void setEpisodes(List<Episode> episodeList) {
        if (episodeList == null || episodeList.isEmpty()) {
//            episode is null
        } else {
            for (Episode item : episodeList)
                onBindViewHolder(onCreateViewHolder(mEpisodeLinearLayout), item);
        }
    }

    private ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.item_onairfragment, null);
        parent.addView(view);
        return new ViewHolder(view);
    }

    private void onBindViewHolder(ViewHolder holder, Episode item) {
        holder.mTextView.setText(item.getNameCn());
        String host=sharedPreferences.getString(PreferenceManager.Global.STR_KEY_HOST,"");
        Glide.with(this).load(host+"/"+item.getThumbnail()).into(holder.mImageView);
    }

    private static final class ViewHolder {
        final View itemView;
        final ImageView mImageView;
        final TextView mTextView;

        public ViewHolder(View view) {
            itemView = view;
            mTextView = (TextView) view.findViewById(R.id.item_descript);
            mImageView = (ImageView) view.findViewById(R.id.item_album);
        }
    }
}

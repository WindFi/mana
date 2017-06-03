package me.sunzheng.mana.home.bangumi;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import me.sunzheng.mana.BangumiDetailsActivity;
import me.sunzheng.mana.R;
import me.sunzheng.mana.home.HomeContract;

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

    RecyclerView mRecyclerView;

    HomeContract.Bangumi.Presenter mPresenter;

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
        mRecyclerView=(RecyclerView) view.findViewById(R.id.bangumidetails_recyclerview);
        // TODO: 2017/5/27  test code
        Glide.with(this).load(getArguments().getString("imageurl")).into(mImageView);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mImageView.setTransitionName("image");
        }
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
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (mRecyclerView == null)
            return;
        if (mRecyclerView.getAdapter() == null)
            mRecyclerView.setAdapter(adapter);
        // TODO: 2017/5/27  test code
        if (mRecyclerView.getLayoutManager() == null)
            mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
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
}

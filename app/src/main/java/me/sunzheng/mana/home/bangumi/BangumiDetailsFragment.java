package me.sunzheng.mana.home.bangumi;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import me.sunzheng.mana.BangumiDetailsActivity;
import me.sunzheng.mana.FavoriteCompact;
import me.sunzheng.mana.R;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.utils.PreferenceManager;


public class BangumiDetailsFragment extends Fragment implements HomeContract.Bangumi.View {
    ImageView mBannerImageView;
    CollapsingToolbarLayout mHeaderCollapsingToolbarLayout;
    Toolbar mToolbar;

    ImageView mImageView;
    TextView mCNTitleTextView;
    TextView mOriginTitleTextView;
    TextView mSummaryTextView;
    TextView mAirDateTextView;
    TextView mWeekDayTextView;
    TextView mFavoriteStatusTextView;
    TextView mEpisodeLabelTextView;
    ContentLoadingProgressBar mContentLoadingProgressBar;
    RecyclerView mRecyclerView;

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
        sharedPreferences = getActivity().getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    @Override
    public android.view.View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bangumidetails, container, false);
    }

    @Override
    public void onViewCreated(android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initToolbar(view);
        initContent(view);
    }

    private ActionBar getSupportActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    private void setSupportActionBar(Toolbar toolbar) {
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

    }

    private void initToolbar(View view) {
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mBannerImageView = (ImageView) view.findViewById(R.id.banner_imageview);
        mHeaderCollapsingToolbarLayout = (CollapsingToolbarLayout) view.findViewById(R.id.header_collaspingtoolbarlayout);
        Glide.with(this).load(getArguments().getString(BangumiDetailsActivity.ARGS_ABLUM_URL_STR)).into(mBannerImageView);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getArguments().getString(BangumiDetailsActivity.ARGS_TITLE_STR));
    }

    private void initContent(View view) {
        mImageView = (ImageView) view.findViewById(R.id.bangumidetails_ablum_imageview);
        mCNTitleTextView = (TextView) view.findViewById(R.id.bangumidetails_name_textview);
        mOriginTitleTextView = (TextView) view.findViewById(R.id.bangumidetails_originname_textview);
        mSummaryTextView = (TextView) view.findViewById(R.id.bangumidetails_summary_textview);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mFavoriteStatusTextView = (TextView) view.findViewById(R.id.bangumidetails_faviortestatus_textview);
        mContentLoadingProgressBar = (ContentLoadingProgressBar) view.findViewById(R.id.bangumidetails_progreassbar);
        mEpisodeLabelTextView = (TextView) view.findViewById(R.id.bangumidetails_episode_label_textview);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mImageView.setTransitionName(BangumiDetailsActivity.PAIR_IMAGE_STR);
        }
        Glide.with(this).load(getArguments().getString(BangumiDetailsActivity.ARGS_ABLUM_URL_STR)).into(mImageView);
        setName(getArguments().getString(BangumiDetailsActivity.ARGS_TITLE_STR));

        mFavoriteStatusTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(getContext(), mFavoriteStatusTextView);
                popupMenu.inflate(R.menu.favorite);
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getTitle().equals(mFavoriteStatusTextView.getText()))
                            return false;
                        int status = 0;
                        if (item.getItemId() == R.id.pop_uncollection) {
                            status = 0;
                        } else if (item.getItemId() == R.id.pop_wish) {
                            status = 1;
                        } else if (item.getItemId() == R.id.pop_watched) {
                            status = 2;
                        } else if (item.getItemId() == R.id.pop_watching) {
                            status = 3;
                        } else if (item.getItemId() == R.id.pop_pause) {
                            status = 4;
                        } else if (item.getItemId() == R.id.pop_abanoned) {
                            status = 5;
                        }
                        mPresenter.changeBangumiFavoriteState(getArguments().getString(BangumiDetailsActivity.ARGS_ID_STR), status);
                        return true;
                    }
                });
                popupMenu.show();
            }
        });
        mRecyclerView.setNestedScrollingEnabled(false);
        mPresenter.load(getArguments().getString(BangumiDetailsActivity.ARGS_ID_STR));

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPresenter.subscribe();
        if (mContentLoadingProgressBar != null)
            mContentLoadingProgressBar.onAttachedToWindow();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mPresenter != null)
            mPresenter.unsubscribe();
        if (mContentLoadingProgressBar != null)
            mContentLoadingProgressBar.onDetachedFromWindow();
    }

    @Override
    public void setPresenter(HomeContract.Bangumi.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public void setWeekDay(long updateDate) {
        if (mWeekDayTextView == null)
            return;
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
    public void setFavouriteStatus(long status) {
        FavoriteCompact.setFavorite(status, mFavoriteStatusTextView);
    }

    @Override
    public void setName(CharSequence name) {
        if (mCNTitleTextView == null)
            return;
        mCNTitleTextView.setText(name);
    }

    @Override
    public void setEpisode(int eps_now, int eps) {
        CharSequence charSequence = String.format(getString(R.string.title_episode_textview), eps_now, eps);
        mEpisodeLabelTextView.setText(charSequence);
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (mRecyclerView == null)
            return;
        if (mRecyclerView.getAdapter() == null)
            mRecyclerView.setAdapter(adapter);
        else
            mRecyclerView.swapAdapter(adapter, true);
        if (mRecyclerView.getLayoutManager() == null)
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public void showProgressIntractor(boolean active) {
        if (mContentLoadingProgressBar == null)
            return;
        if (active)
            mContentLoadingProgressBar.show();
        else
            mContentLoadingProgressBar.hide();
    }


}

package me.sunzheng.mana.home.mybangumi;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.sunzheng.mana.R;
import me.sunzheng.mana.home.HomeContract;

/**
 * Created by Sun on 2017/6/22.
 */

public class FavoriteFragment extends Fragment implements HomeContract.MyBangumi.View {
    RecyclerView mRecyclerView;
    SwipeRefreshLayout mSwipeRefreshLayout;
    HomeContract.MyBangumi.Presenter mPresenter;

    public FavoriteFragment() {
    }

    public static FavoriteFragment newInstance() {
        FavoriteFragment fragment = new FavoriteFragment();

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_myfavorites, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        mSwipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefreshlayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPresenter.load();
            }
        });
        mPresenter.load();
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
    public void onDestroy() {
        super.onDestroy();
        mPresenter.unsubscribe();
    }

    @Override
    public void setPresenter(HomeContract.MyBangumi.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void showProgressIntractor(boolean active) {
        if (mSwipeRefreshLayout == null)
            return;
        mSwipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void showEmpty() {

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
}

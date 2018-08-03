package me.sunzheng.mana.home.mybangumi;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import me.sunzheng.mana.R;
import me.sunzheng.mana.home.HomeContract;

/**
 * Created by Sun on 2017/6/22.
 */

public class FavoritesFragment extends Fragment implements HomeContract.MyBangumi.View {
    RecyclerView mRecyclerView;
    SwipeRefreshLayout mSwipeRefreshLayout;
    HomeContract.MyBangumi.Presenter mPresenter;

    public FavoritesFragment() {
    }

    public static FavoritesFragment newInstance() {
        FavoritesFragment fragment = new FavoritesFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_myfavorites, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mSwipeRefreshLayout = view.findViewById(R.id.swiperefreshlayout);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mSwipeRefreshLayout.setEnabled(false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (mPresenter != null)
            mPresenter.subscribe();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mPresenter != null)
            mPresenter.unsubscribe();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
    public void onFilter(int status) {
        mPresenter.setFilter(status);
    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (mRecyclerView == null)
            return;
        if (mRecyclerView.getAdapter() == null) {
            mRecyclerView.setAdapter(adapter);
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        } else
            mRecyclerView.swapAdapter(adapter, true);
        if (mRecyclerView.getLayoutManager() == null)
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
    }
}

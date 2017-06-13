package me.sunzheng.mana.home.search.wrapper;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import me.sunzheng.mana.R;
import me.sunzheng.mana.home.HomeContract;

/**
 * Created by Sun on 2017/6/9.
 */

public class SearchResultFragment extends Fragment implements HomeContract.Search.View {
    HomeContract.Search.Presenter mPresenter;
    RecyclerView mRecyclerView;

    public static SearchResultFragment newInstance(Bundle extras) {
        SearchResultFragment fragment = new SearchResultFragment();
        fragment.setArguments(extras);
        return fragment;
    }

    @Override
    public void setPresenter(HomeContract.Search.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search_result, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
    }

    @Override
    public void empty(String message) {

    }

    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (mRecyclerView.getAdapter() == null) {
            mRecyclerView.setAdapter(adapter);
            mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL));
        } else {
            mRecyclerView.swapAdapter(adapter, false);
        }
        if (mRecyclerView.getLayoutManager() == null)
            mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

    }

    @Override
    public void notifyDataSetChanged() {

    }
}

package me.sunzheng.mana.home.onair;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import me.sunzheng.mana.R;
import me.sunzheng.mana.core.BangumiModel;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.widget.EmptyAdapter;

/**
 * A fragment representing a list of Items.
 */
public class OnAirFragment extends Fragment implements HomeContract.OnAir.View {
    public static final int INT_TYPE_ANIMATION = 2;
    public static final int INT_TYPE_DRAMA = 6;
    public static final String INT_ARGS_TYPE = "air_type";
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    int type;
    HomeContract.OnAir.Presenter mPresenter;
    SwipeRefreshLayout swipeRefreshLayout;
    RecyclerView recyclerView;

    public OnAirFragment() {
    }

    @SuppressWarnings("unused")
    public static OnAirFragment newInstance(int type) {
        OnAirFragment fragment = new OnAirFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(INT_ARGS_TYPE, type);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null)
            savedInstanceState = getArguments();
        type = savedInstanceState.getInt(INT_ARGS_TYPE);
    }

    @Override
    public android.view.View onCreateView(LayoutInflater inflater, ViewGroup container,
                                          Bundle savedInstanceState) {
        android.view.View view = inflater.inflate(R.layout.fragment_item_list, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.list);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swiperefreshlayout);
        return view;
    }

    @Override
    public void onViewCreated(android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mPresenter.load(type);
            }
        });
        showProgressIntractor(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void setPresenter(HomeContract.OnAir.Presenter presenter) {
        this.mPresenter = presenter;
    }

    @Override
    public void showProgressIntractor(boolean active) {
        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setRefreshing(active);
    }

    @Override
    public void showToast(String errorMessage) {
        Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void setAirs(List<BangumiModel> bangumiModels) {
        if (recyclerView.getAdapter() == null || recyclerView.getAdapter() instanceof EmptyAdapter)
            recyclerView.setAdapter(new OnAirItemRecyclerViewAdapter(bangumiModels));
        else
            recyclerView.swapAdapter(new OnAirItemRecyclerViewAdapter(bangumiModels), true);
        if (recyclerView.getLayoutManager() == null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
            DividerItemDecoration itemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL);
            recyclerView.addItemDecoration(itemDecoration);
        }
    }

    @Override
    public Context getContext() {
        return getActivity();
    }
}

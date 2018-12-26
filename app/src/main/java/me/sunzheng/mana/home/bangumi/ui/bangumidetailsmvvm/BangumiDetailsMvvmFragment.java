package me.sunzheng.mana.home.bangumi.ui.bangumidetailsmvvm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import me.sunzheng.mana.R;
import me.sunzheng.mana.databinding.BangumiDetailsMvvmFragmentBinding;

public class BangumiDetailsMvvmFragment extends Fragment {

    private BangumiDetailsMvvmViewModel mViewModel;

    public static BangumiDetailsMvvmFragment newInstance() {
        return new BangumiDetailsMvvmFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        BangumiDetailsMvvmFragmentBinding binding = DataBindingUtil.inflate(inflater, R.layout.bangumi_details_mvvm_fragment, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(BangumiDetailsMvvmViewModel.class);
        // TODO: Use the ViewModel
    }

}

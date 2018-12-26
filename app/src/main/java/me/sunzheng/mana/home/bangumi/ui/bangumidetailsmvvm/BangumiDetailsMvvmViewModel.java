package me.sunzheng.mana.home.bangumi.ui.bangumidetailsmvvm;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class BangumiDetailsMvvmViewModel extends ViewModel {
    public MutableLiveData<String> name;
    public MutableLiveData<String> nameCn;
    public MutableLiveData<String> episode;
    public MutableLiveData<Integer> status;
    public MutableLiveData<String> summary;
    public MutableLiveData<String> createTime;
    public MutableLiveData<String> dayInWeek;
}

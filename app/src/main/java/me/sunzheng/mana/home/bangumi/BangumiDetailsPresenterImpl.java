package me.sunzheng.mana.home.bangumi;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper;
import me.sunzheng.mana.home.bangumi.wrapper.Data;
import me.sunzheng.mana.home.bangumi.wrapper.Episode;

/**
 * Created by Sun on 2017/5/27.
 */

public class BangumiDetailsPresenterImpl implements HomeContract.Bangumi.Presenter {
    CompositeDisposable completable;
    HomeContract.Bangumi.View mView;
    HomeApiService.Bangumi apiServices;

    public BangumiDetailsPresenterImpl(HomeContract.Bangumi.View view, HomeApiService.Bangumi apiServices) {
        this.mView = view;
        this.apiServices = apiServices;
        completable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {
        completable.clear();
    }

    @Override
    public void load(String id) {
        mView.showProgressIntractor(true);
        Disposable disposable = apiServices.getBangumiDetail(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BangumiDetailWrapper>() {
                    @Override
                    public void accept(BangumiDetailWrapper bangumiDetailWrapper) throws Exception {
                        Data mData = bangumiDetailWrapper.getData();
                        mView.setAirDate(mData.getAirDate());
                        mView.setSummary(mData.getSummary());
                        mView.setOriginName(mData.getName());
                        mView.setFaviorStatus(mData.getFavoriteStatus());
                        mView.setOriginName(mData.getNameCn());
                        //filter for unavailable
                        List<Episode> _list = new ArrayList<>();
                        for(Episode item:mData.getEpisodes()){
                            if(item.getStatus()!=0L)
                                _list.add(item);
                        }
                        mData.setEpisodes(_list);
                        Collections.sort(mData.getEpisodes(), new Comparator<Episode>() {
                            @Override
                            public int compare(Episode o1, Episode o2) {
                                return (int) (o2.getEpisodeNo() - o1.getEpisodeNo());
                            }
                        });
                        mView.setEpisodes(mData.getEpisodes());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e("exception:", throwable.getLocalizedMessage());
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        mView.showProgressIntractor(false);
                    }
                });
        completable.add(disposable);
    }
}

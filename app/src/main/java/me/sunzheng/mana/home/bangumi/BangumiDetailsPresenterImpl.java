package me.sunzheng.mana.home.bangumi;

import android.util.Log;

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
                        mView.setAdapter(new EpisodeAdapter(mData.getEpisodes()));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e("exception:", throwable.getLocalizedMessage());
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {

                    }
                });
        completable.add(disposable);
    }
}

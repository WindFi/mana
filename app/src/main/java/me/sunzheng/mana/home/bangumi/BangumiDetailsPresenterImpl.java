package me.sunzheng.mana.home.bangumi;

import android.text.TextUtils;
import android.util.Log;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.home.FavoriteStatusRequest;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper;
import me.sunzheng.mana.home.bangumi.wrapper.Data;
import me.sunzheng.mana.home.bangumi.wrapper.Episode;

/**
 * Created by Sun on 2017/5/27.
 */

public class BangumiDetailsPresenterImpl implements HomeContract.Bangumi.Presenter {
    final String TAG = getClass().getSimpleName();
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
                .delay(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<BangumiDetailWrapper>() {
                    @Override
                    public void accept(BangumiDetailWrapper bangumiDetailWrapper) throws Exception {
                        final Data mData = bangumiDetailWrapper.getData();
                        mView.setAirDate(mData.getAirDate());
                        mView.setSummary(mData.getSummary());
                        mView.setFavouriteStatus(mData.getFavoriteStatus());
                        mView.setOriginName(TextUtils.isEmpty(mData.getName()) ? mData.getNameCn() : mData.getName());
                        Observable.fromIterable(mData.getEpisodes()).filter(new Predicate<Episode>() {
                            @Override
                            public boolean test(Episode episode) throws Exception {
                                return episode.getStatus() != 0L;
                            }
                        }).toSortedList(new Comparator<Episode>() {
                            @Override
                            public int compare(Episode o1, Episode o2) {
                                return (int) (o2.getEpisodeNo() - o1.getEpisodeNo());
                            }
                        }).subscribe(new SingleObserver<List<Episode>>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onSuccess(List<Episode> value) {
                                mView.setEpisode(value.size(), mData.getEps());
                                mView.setAdapter(new EpisodeAdapter(value));
                            }

                            @Override
                            public void onError(Throwable e) {

                            }
                        });
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
        mView.showProgressIntractor(true);
        completable.add(disposable);
    }

    @Override
    public void changeBangumiFavoriteState(String id, final int status) {
        FavoriteStatusRequest request = new FavoriteStatusRequest();
        request.status = status;
        Disposable disposable = apiServices.changeBangumiFavoriteStatus(id, request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Response>() {
                    @Override
                    public void accept(Response response) throws Exception {
                        if (response.status == 0) {
                            mView.setFavouriteStatus(status);
                        } else {
                            Log.i(TAG, response.message);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.getLocalizedMessage());
                    }
                });
        completable.add(disposable);
    }

}

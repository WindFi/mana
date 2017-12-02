package me.sunzheng.mana.home.bangumi;

import android.text.TextUtils;
import android.util.Log;

import java.util.Comparator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.core.Episode;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.bangumi.respository.DataRespository;
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper;
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetails;

/**
 * Created by Sun on 2017/5/27.
 */

public class BangumiDetailsPresenterImpl implements HomeContract.Bangumi.Presenter {
    final String TAG = getClass().getSimpleName();
    CompositeDisposable completable;
    HomeContract.Bangumi.View mView;
    DataRespository dataRespository;
    BangumiDetailWrapper data;

    public BangumiDetailsPresenterImpl(HomeContract.Bangumi.View view, DataRespository dataRespository) {
        this.mView = view;
        this.dataRespository = dataRespository;
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
    public void load() {
        Disposable disposable = dataRespository.query()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        mView.showProgressIntractor(false);
                    }
                })
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        mView.showProgressIntractor(false);
                    }
                })
                .doOnNext(new Consumer<BangumiDetailWrapper>() {
                    @Override
                    public void accept(BangumiDetailWrapper bangumiDetailWrapper) throws Exception {
                        final BangumiDetails mBangumiDetails = bangumiDetailWrapper.getBangumiDetails();
                        if (mBangumiDetails == null)
                            return;
                        data = bangumiDetailWrapper;
                        mView.setAirDate(mBangumiDetails.getAirDate());
                        mView.setSummary(mBangumiDetails.getSummary());
                        mView.setFavouriteStatus(mBangumiDetails.getFavoriteStatus());
                        mView.setOriginName(TextUtils.isEmpty(mBangumiDetails.getName()) ? mBangumiDetails.getNameCn() : mBangumiDetails.getName());
                        Observable.fromIterable(mBangumiDetails.getEpisodes()).filter(new Predicate<Episode>() {
                            @Override
                            public boolean test(Episode episode) throws Exception {
                                return episode.getStatus() != 0L;
                            }
                        }).toSortedList(new Comparator<Episode>() {
                            @Override
                            public int compare(Episode o1, Episode o2) {
                                return o2.getEpisodeNo() - o1.getEpisodeNo();
                            }
                        }).subscribe(new SingleObserver<List<Episode>>() {
                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onSuccess(List<Episode> value) {
                                mView.setEpisode(value.size(), mBangumiDetails.getEps());
                                mView.setAdapter(new EpisodeAdapter(value));
                            }

                            @Override
                            public void onError(Throwable e) {

                            }
                        });
                    }
                }).subscribe();
        mView.showProgressIntractor(true);
        completable.add(disposable);
    }

    @Override
    public void changeBangumiFavoriteState(int status) {
        final int originStatus = data.getBangumiDetails().getFavoriteStatus();
        data.getBangumiDetails().setFavoriteStatus(status);
        Disposable disposable = dataRespository.update(data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action() {
                    @Override
                    public void run() throws Exception {
                        mView.setFavouriteStatus(data.getBangumiDetails().getFavoriteStatus());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.getLocalizedMessage());
                        data.getBangumiDetails().setStatus(originStatus);
                    }
                });
        completable.add(disposable);
    }

}

package me.sunzheng.mana.home.mybangumi;

import android.util.Log;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.mybangumi.wrapper.FaviourWrapper;
import me.sunzheng.mana.home.onair.OnAirItemRecyclerViewAdapter;

/**
 * Created by Sun on 2017/7/17.
 */

public class MyFavouritePresenter implements HomeContract.MyBangumi.Presenter {
    final String TAG = getClass().getSimpleName();
    HomeApiService.MyBangumi apiService;
    CompositeDisposable compositeDisposable;
    HomeContract.MyBangumi.View mView;

    public MyFavouritePresenter(HomeContract.MyBangumi.View mView, HomeApiService.MyBangumi apiService) {
        this.apiService = apiService;
        this.mView = mView;
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {
        compositeDisposable.clear();
    }

    @Override
    public void load() {
        mView.showProgressIntractor(true);
        Disposable disposable = apiService.listMyBangumi()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<FaviourWrapper>() {
                    @Override
                    public void accept(FaviourWrapper faviourWrapper) throws Exception {
                        faviourWrapper.getData();
                        if (faviourWrapper == null || faviourWrapper.getData().isEmpty()) {
                            mView.showEmpty();
                        }
                        mView.setAdapter(new OnAirItemRecyclerViewAdapter(faviourWrapper.getData()));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.e(TAG, throwable.getLocalizedMessage());
                        mView.showProgressIntractor(false);
                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        mView.showProgressIntractor(false);
                    }
                });
        compositeDisposable.add(disposable);
    }

    @Override
    public void setFilter(int status) {

    }
}

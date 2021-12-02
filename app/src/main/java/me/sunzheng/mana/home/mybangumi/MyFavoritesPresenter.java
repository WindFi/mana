package me.sunzheng.mana.home.mybangumi;

import android.util.Log;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.core.BangumiModel;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.mybangumi.respository.DataRespository;

/**
 * Created by Sun on 2017/7/17.
 */

public class MyFavoritesPresenter implements HomeContract.MyBangumi.Presenter {
    final String TAG = getClass().getSimpleName();
    CompositeDisposable compositeDisposable;
    HomeContract.MyBangumi.View mView;
    DataRespository dataRespository;

    public MyFavoritesPresenter(HomeContract.MyBangumi.View mView, DataRespository dataRespository) {
        this.mView = mView;
        compositeDisposable = new CompositeDisposable();
        this.dataRespository = dataRespository;
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

    }

    @Override
    public void setFilter(int status) {
        mView.showProgressIntractor(true);
        Disposable disposable = dataRespository.query(status)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<BangumiModel>>() {
                    @Override
                    public void accept(List<BangumiModel> bangumiModels) throws Exception {
                        if (bangumiModels == null || bangumiModels.isEmpty())
                            mView.showEmpty();
                        else {
//                            mView.setAdapter(new OnAirItemRecyclerViewAdapter(bangumiModels));
                        }
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
}

package me.sunzheng.mana.home.main;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;

/**
 * Created by Sun on 2018/3/12.
 */

public class AnnoucePresenterImpl implements HomeContract.Annouce.Presenter {
    HomeApiService.Announce apiService;
    CompositeDisposable compositeDisposable;
    HomeContract.Annouce.View mView;

    public AnnoucePresenterImpl(HomeContract.Annouce.View mView, HomeApiService.Announce apiService) {
        this.apiService = apiService;
        this.mView = mView;
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {
        if (compositeDisposable != null)
            compositeDisposable.clear();
    }

    @Override
    public void load() {
        mView.showProgressIntractor(true);
        Disposable disposable = apiService.getAllAvailable().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ResponseWrapper>() {
                    @Override
                    public void accept(ResponseWrapper responseWrapper) throws Exception {
                        if (responseWrapper == null || responseWrapper.data == null || responseWrapper.data.isEmpty()) {
                            mView.showContentView(false);
                            return;
                        }
                        mView.setData(responseWrapper.data);
                        mView.showContentView(true);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
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
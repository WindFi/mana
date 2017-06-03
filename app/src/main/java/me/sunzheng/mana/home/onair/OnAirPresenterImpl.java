package me.sunzheng.mana.home.onair;

import android.util.Log;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.onair.wrapper.AirWrapper;

/**
 * Created by Sun on 2017/5/24.
 */

public class OnAirPresenterImpl implements HomeContract.OnAir.Presenter {
    HomeContract.OnAir.View view;
    HomeApiService.OnAir apiServices;
    CompositeDisposable compositeDisposable;
    OnAirItemRecyclerViewAdapter adapter;
    public OnAirPresenterImpl(HomeContract.OnAir.View view, HomeApiService.OnAir apiServices) {
        this.view = view;
        this.apiServices = apiServices;
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {
        compositeDisposable.dispose();
    }

    @Override
    public void load(int type) {
        view.showProgressIntractor(true);
        Disposable disposable = apiServices.listAll(type)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<AirWrapper>() {
                    @Override
                    public void accept(AirWrapper air) throws Exception {
                        if(air.getData()!=null&&air.getData().size()>0)
                        view.setAdapter(new OnAirItemRecyclerViewAdapter(air.getData()));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.i("e:",throwable.getLocalizedMessage());

                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        view.showProgressIntractor(false);
                    }
                });
        compositeDisposable.add(disposable);
    }
}

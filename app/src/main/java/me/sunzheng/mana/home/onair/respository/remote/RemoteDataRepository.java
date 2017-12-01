package me.sunzheng.mana.home.onair.respository.remote;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.onair.respository.DataRepository;
import me.sunzheng.mana.home.onair.wrapper.AirWrapper;

/**
 * Created by Sun on 2017/10/22.
 */

public class RemoteDataRepository implements DataRepository {
    HomeApiService.OnAir apiService;

    public RemoteDataRepository(HomeApiService.OnAir apiService) {
        this.apiService = apiService;
    }

    @Override
    public Observable<AirWrapper> query(int type) {
        return apiService.listAll(type).toObservable();
    }

    @Override
    public Completable insert(AirWrapper o) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {

            }
        });
    }

    @Override
    public Completable delete(AirWrapper o) {
        return null;
    }

    @Override
    public Completable update(AirWrapper o) {
        return null;
    }
}

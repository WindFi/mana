package me.sunzheng.mana.home.onair.respository.remote;

import java.util.ArrayList;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
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
        return Maybe.fromSingle(apiService.listAll(type)).flattenAsObservable(new Function<AirWrapper, Iterable<? extends AirWrapper>>() {
            @Override
            public Iterable<? extends AirWrapper> apply(AirWrapper airWrapper) throws Exception {
                ArrayList<AirWrapper> a = new ArrayList<>();
                a.add(airWrapper);
                return a;
            }
        });
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

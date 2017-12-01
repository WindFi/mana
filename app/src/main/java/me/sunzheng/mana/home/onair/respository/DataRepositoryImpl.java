package me.sunzheng.mana.home.onair.respository;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.onair.respository.local.LocalDataRepository;
import me.sunzheng.mana.home.onair.respository.remote.RemoteDataRepository;
import me.sunzheng.mana.home.onair.wrapper.AirWrapper;

/**
 * Created by Sun on 2017/10/22.
 */

public class DataRepositoryImpl implements DataRepository {
    Context mContext;
    DataRepository remoteDataRepository;
    Map<Integer, DataRepository> map;

    public DataRepositoryImpl(Context context, HomeApiService.OnAir apiService) {
        map = new HashMap<>(2);
        this.mContext = context;
        remoteDataRepository = new RemoteDataRepository(apiService);
    }


    @Override
    public Observable<AirWrapper> query(final int type) {
        if (!map.containsKey(type))
            map.put(type, new LocalDataRepository(mContext, type));
        final Observable<AirWrapper> local = map.get(type).query(type);
        return queryRemoteAndCache(type).firstOrError().toObservable().concatMapDelayError(new Function<AirWrapper, ObservableSource<? extends AirWrapper>>() {
            @Override
            public ObservableSource<? extends AirWrapper> apply(AirWrapper airWrapper) throws Exception {
                return local.filter(new Predicate<AirWrapper>() {
                    @Override
                    public boolean test(AirWrapper airWrapper) throws Exception {
                        return airWrapper != null;
                    }
                });
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
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {

            }
        });
    }

    @Override
    public Completable update(AirWrapper o) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {

            }
        });
    }

    private Observable<AirWrapper> queryRemoteAndCache(final int type) {
        return remoteDataRepository.query(type).doOnNext(new Consumer<AirWrapper>() {
            @Override
            public void accept(AirWrapper airWrapper) throws Exception {
                if (map.containsKey(type)) {
                    map.get(type).insert(airWrapper).subscribe();
                } else {
                    map.put(type, new LocalDataRepository(mContext, type)).insert(airWrapper).subscribe();
                }
            }
        });
    }
}

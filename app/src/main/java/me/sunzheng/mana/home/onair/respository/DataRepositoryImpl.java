package me.sunzheng.mana.home.onair.respository;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Notification;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
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
        //see https://stackoverflow.com/questions/45216713/using-zip-with-a-maybe-that-may-not-emit-a-value
        return Observable.zip(map.get(type).query(type).materialize(), remoteDataRepository.query(type).materialize(), new BiFunction<Notification<AirWrapper>, Notification<AirWrapper>, AirWrapper>() {
            @Override
            public AirWrapper apply(Notification<AirWrapper> airWrapperNotification, Notification<AirWrapper> airWrapperNotification2) throws Exception {
                if (airWrapperNotification.getValue() == null) {
                    if (airWrapperNotification2.getValue() == null) {
                        return new AirWrapper();
                    } else {
                        queryRemoteAndCache(airWrapperNotification2.getValue(), type).subscribe();
                        return airWrapperNotification2.getValue();
                    }
                } else {
                    if (airWrapperNotification.getValue() == null)
                        throw new NullPointerException("");
                    else
                        return airWrapperNotification.getValue();
                }
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

    private Completable queryRemoteAndCache(AirWrapper o, final int type) {
        return map.get(type).insert(o);
    }
}

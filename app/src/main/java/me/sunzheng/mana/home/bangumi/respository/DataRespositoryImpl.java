package me.sunzheng.mana.home.bangumi.respository;

import android.content.Context;

import io.reactivex.Completable;
import io.reactivex.Notification;
import io.reactivex.Observable;
import io.reactivex.functions.BiFunction;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.bangumi.respository.local.LocalDataRespository;
import me.sunzheng.mana.home.bangumi.respository.remote.RemoteDataRespository;
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper;

/**
 * Created by Sun on 2017/10/27.
 */

public class DataRespositoryImpl implements DataRespository {
    DataRespository local, remote;

    public DataRespositoryImpl(Context context, HomeApiService.Bangumi apiService, String id) {
        local = new LocalDataRespository(context, id);
        remote = new RemoteDataRespository(apiService, id);
    }

    @Override
    public Observable<BangumiDetailWrapper> query() {
        return Observable.zip(local.query().materialize(), remote.query().materialize(), new BiFunction<Notification<BangumiDetailWrapper>, Notification<BangumiDetailWrapper>, BangumiDetailWrapper>() {
            @Override
            public BangumiDetailWrapper apply(Notification<BangumiDetailWrapper> bangumiDetailWrapperNotification, Notification<BangumiDetailWrapper> bangumiDetailWrapperNotification2) throws Exception {
                if (bangumiDetailWrapperNotification2.getValue() != null) {
                    insert(bangumiDetailWrapperNotification2.getValue()).subscribe();
                    if (bangumiDetailWrapperNotification.getValue() != null)
                        return bangumiDetailWrapperNotification.getValue();
                }
                return new BangumiDetailWrapper();
            }
        });
    }

    @Override
    public Completable insert(BangumiDetailWrapper bangumiDetailWrapper) {
        return local.insert(bangumiDetailWrapper);
    }

    @Override
    public Completable update(BangumiDetailWrapper bangumiDetailWrapper) {
        return remote.update(bangumiDetailWrapper).andThen(local.update(bangumiDetailWrapper));
    }

    @Override
    public Completable delete() {
        return local.delete();
    }
}

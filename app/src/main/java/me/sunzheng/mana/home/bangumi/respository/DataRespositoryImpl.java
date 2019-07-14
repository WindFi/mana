package me.sunzheng.mana.home.bangumi.respository;

import android.content.Context;
import android.util.Log;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
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
        return queryRemoteAndCache().firstOrError().toObservable().onErrorResumeNext(queryFromLocal());
    }

    private Observable<BangumiDetailWrapper> queryFromLocal() {
        return local.query().firstOrError().toObservable().doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Log.e(DataRespositoryImpl.this.getClass().getSimpleName(), "no cached");
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

    private Observable<BangumiDetailWrapper> queryRemoteAndCache() {
        return remote.query().doOnNext(new Consumer<BangumiDetailWrapper>() {
            @Override
            public void accept(BangumiDetailWrapper bangumiDetailWrapper) throws Exception {
                local.insert(bangumiDetailWrapper).subscribe();
            }
        });
    }
}

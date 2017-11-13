package me.sunzheng.mana.home.mybangumi.respository;

import android.content.Context;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.mybangumi.respository.local.LocalDataRepository;
import me.sunzheng.mana.home.mybangumi.respository.remote.RemoteRespository;
import me.sunzheng.mana.home.onair.wrapper.BangumiModel;

/**
 * Created by Sun on 2017/11/13.
 */

public class DataRespositoryImpl implements DataRespository {
    DataRespository local, remote;

    public DataRespositoryImpl(Context context, HomeApiService.MyBangumi apiService) {
        local = new LocalDataRepository(context);
        remote = new RemoteRespository(apiService);
    }

    @Override
    public Observable<List<BangumiModel>> query(int status) {
        if (status == 0) {
            local.delete(null).toObservable().subscribe();
            return Observable.concat(queryFromRemoteAndSave(), local.query(status));
        } else
            return local.query(status);
    }

    @Override
    public Completable delete(BangumiModel object) {
        if (object == null)
            return Completable.complete();
        else
            return remote.delete(object).andThen(local.delete(object));
    }

    @Override
    public Completable insert(BangumiModel object) {
        return remote.insert(object).andThen(local.insert(object));
    }

    @Override
    public Completable update(BangumiModel object) {
        return remote.update(object).andThen(local.update(object));
    }

    private Observable queryFromRemoteAndSave() {
        return remote.query(0).doOnNext(new Consumer<List<BangumiModel>>() {
            @Override
            public void accept(List<BangumiModel> bangumiModels) throws Exception {
                for (BangumiModel items : bangumiModels)
                    local.update(items).subscribe();
            }
        });
    }
}

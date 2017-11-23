package me.sunzheng.mana.home.mybangumi.respository.remote;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.mybangumi.respository.DataRespository;
import me.sunzheng.mana.home.mybangumi.wrapper.FavoriteWrapper;
import me.sunzheng.mana.home.onair.wrapper.BangumiModel;

/**
 * Created by Sun on 2017/11/13.
 */

public class RemoteRespository implements DataRespository {
    HomeApiService.MyBangumi apiService;

    public RemoteRespository(HomeApiService.MyBangumi apiService) {
        this.apiService = apiService;
    }

    @Override
    public Observable<List<BangumiModel>> query(int status) {
        return apiService.listMyBangumi(status).flatMapObservable(new Function<FavoriteWrapper, ObservableSource<? extends List<BangumiModel>>>() {
            @Override
            public ObservableSource<? extends List<BangumiModel>> apply(FavoriteWrapper favoriteWrapper) throws Exception {
                return Observable.just(favoriteWrapper.getData());
            }
        });
    }

    @Override
    public Completable delete(BangumiModel object) {
        return Completable.complete();
    }

    @Override
    public Completable insert(BangumiModel object) {
        return Completable.complete();
    }

    @Override
    public Completable update(BangumiModel object) {
        return Completable.complete();
    }
}

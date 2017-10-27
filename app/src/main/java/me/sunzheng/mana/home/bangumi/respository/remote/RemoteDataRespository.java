package me.sunzheng.mana.home.bangumi.respository.remote;

import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.CompletableOnSubscribe;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import me.sunzheng.mana.home.FavoriteStatusRequest;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.bangumi.Response;
import me.sunzheng.mana.home.bangumi.respository.DataRespository;
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper;

/**
 * Created by Sun on 2017/10/27.
 */

public class RemoteDataRespository implements DataRespository {
    HomeApiService.Bangumi apiService;
    String id;

    public RemoteDataRespository(HomeApiService.Bangumi apiService, String id) {
        this.apiService = apiService;
        this.id = id;
    }

    @Override
    public Observable<BangumiDetailWrapper> query() {
        return apiService.getBangumiDetail(id);
    }

    @Override
    public Completable insert(BangumiDetailWrapper bangumiDetailWrapper) {
        return Completable.complete();
    }

    @Override
    public Completable update(BangumiDetailWrapper bangumiDetailWrapper) {
        final FavoriteStatusRequest request = new FavoriteStatusRequest();
        request.status = (int) bangumiDetailWrapper.getData().getFavoriteStatus();
        return Completable.create(new CompletableOnSubscribe() {
            @Override
            public void subscribe(final CompletableEmitter e) throws Exception {
                apiService.changeBangumiFavoriteStatus(id, request).doOnSuccess(new Consumer<Response>() {
                    @Override
                    public void accept(Response response) throws Exception {
                        if (response.status == 0)
                            e.onComplete();
                        else
                            e.onError(new UnknownError("status change faild"));
                    }
                }).subscribe();
            }
        });
    }

    @Override
    public Completable delete() {
        return Completable.complete();
    }
}

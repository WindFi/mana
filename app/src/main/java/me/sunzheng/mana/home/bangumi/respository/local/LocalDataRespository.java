package me.sunzheng.mana.home.bangumi.respository.local;

import android.content.Context;

import java.util.ArrayList;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import me.sunzheng.mana.home.bangumi.respository.DataRespository;
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper;
import me.sunzheng.mana.utils.CacheFileUntil;

/**
 * Created by Sun on 2017/10/27.
 */

public class LocalDataRespository implements DataRespository {
    CacheFileUntil fileUntil;

    public LocalDataRespository(Context context, String id) {
        this.fileUntil = new CacheFileUntil(context, id + STR_EXTAND_NAME);
    }

    @Override
    public Observable<BangumiDetailWrapper> query() {
        return getBangumiDetails().flattenAsObservable(new Function<BangumiDetailWrapper, Iterable<? extends BangumiDetailWrapper>>() {
            @Override
            public Iterable<? extends BangumiDetailWrapper> apply(BangumiDetailWrapper bangumiDetailWrapper) throws Exception {
                ArrayList<BangumiDetailWrapper> a = new ArrayList<>(1);
                a.add(bangumiDetailWrapper);
                return a;
            }
        });
    }

    @Override
    public Completable insert(final BangumiDetailWrapper bangumiDetailWrapper) {
        return update(bangumiDetailWrapper);
    }

    @Override
    public Completable update(final BangumiDetailWrapper bangumiDetailWrapper) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                fileUntil.insert(bangumiDetailWrapper);
            }
        });
    }

    @Override
    public Completable delete() {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                fileUntil.delete();
            }
        });
    }

    private Maybe<BangumiDetailWrapper> getBangumiDetails() {
        Maybe maybe = Maybe.empty();
        BangumiDetailWrapper bangumiDetailWrapper = fileUntil.query(BangumiDetailWrapper.class);
        if (bangumiDetailWrapper != null) {
            maybe = Maybe.just(bangumiDetailWrapper);
        }
        return maybe;
    }
}


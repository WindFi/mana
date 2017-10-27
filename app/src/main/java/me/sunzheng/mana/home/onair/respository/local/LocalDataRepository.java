package me.sunzheng.mana.home.onair.respository.local;

import android.content.Context;

import java.util.ArrayList;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import me.sunzheng.mana.home.onair.respository.DataRepository;
import me.sunzheng.mana.home.onair.wrapper.AirWrapper;
import me.sunzheng.mana.utils.CachFileUtil;

/**
 * Created by Sun on 2017/10/22.
 */

public class LocalDataRepository implements DataRepository {
    CachFileUtil fileUtil;

    public LocalDataRepository(Context context, int type) {
        fileUtil = new CachFileUtil(context, "OnAir" + type + ".tmp");
    }

    private Maybe<AirWrapper> getAirs() {
        Maybe maybe = Maybe.empty();
        AirWrapper airWrapper = fileUtil.query(AirWrapper.class);
        if (airWrapper != null)
            maybe = Maybe.just(airWrapper);
        return maybe;
    }

    @Override
    public Observable<AirWrapper> query(int type) {
        return getAirs().flattenAsObservable(new Function<AirWrapper, Iterable<? extends AirWrapper>>() {
            @Override
            public Iterable<? extends AirWrapper> apply(AirWrapper airWrapper) throws Exception {
                ArrayList<AirWrapper> a = new ArrayList<>();
                a.add(airWrapper);
                return a;
            }
        });
    }

    @Override
    public Completable insert(final AirWrapper o) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                fileUtil.insert(o);
            }
        });
    }

    @Override
    public Completable delete(AirWrapper o) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                fileUtil.delete();
            }
        });
    }

    @Override
    public Completable update(final AirWrapper o) {
        return Completable.fromAction(new Action() {
            @Override
            public void run() throws Exception {
                fileUtil.insert(o);
            }
        });
    }
}


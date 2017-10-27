package me.sunzheng.mana.home.onair.respository;

import io.reactivex.Completable;
import io.reactivex.Observable;
import me.sunzheng.mana.home.onair.wrapper.AirWrapper;

/**
 * Created by Sun on 2017/10/22.
 */

public interface DataRepository {
    Observable<AirWrapper> query(int type);

    Completable insert(AirWrapper o);

    Completable delete(AirWrapper o);

    Completable update(AirWrapper o);
}

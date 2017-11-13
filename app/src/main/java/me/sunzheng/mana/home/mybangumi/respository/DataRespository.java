package me.sunzheng.mana.home.mybangumi.respository;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import me.sunzheng.mana.home.onair.wrapper.BangumiModel;

/**
 * Created by Sun on 2017/11/7.
 */

public interface DataRespository {
    Observable<List<BangumiModel>> query(int status);

    Completable delete(BangumiModel object);

    Completable insert(BangumiModel object);

    Completable update(BangumiModel object);
}

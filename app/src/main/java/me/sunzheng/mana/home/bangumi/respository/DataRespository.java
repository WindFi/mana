package me.sunzheng.mana.home.bangumi.respository;

import io.reactivex.Completable;
import io.reactivex.Observable;
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper;

/**
 * Created by Sun on 2017/10/27.
 */

public interface DataRespository {
    String STR_EXTAND_NAME = ".tmp";

    Observable<BangumiDetailWrapper> query();

    Completable insert(BangumiDetailWrapper bangumiDetailWrapper);

    Completable update(BangumiDetailWrapper bangumiDetailWrapper);

    Completable delete();
}

package me.sunzheng.mana;

import io.reactivex.disposables.Disposable;

/**
 * Created by Sun on 2017/5/22.
 */

public interface IPresenter{
    void subscribe();

    void unsubscribe();
}

package me.sunzheng.mana.utils;

import android.content.Context;

/**
 * Created by Sun on 2017/5/22.
 */

public interface IView<T extends IPresenter> {
    Context getContext();
    void setPresenter(T presenter);
}

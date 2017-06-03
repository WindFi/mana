package me.sunzheng.mana;

/**
 * Created by Sun on 2017/5/22.
 */

public interface IView<T extends IPresenter> {
    void setPresenter(T presenter);
}

package me.sunzheng.mana.account.config;

import me.sunzheng.mana.account.AccountContrant;

/**
 * Created by Sun on 2017/7/31.
 */

public class StartPresenterImpl implements AccountContrant.Start.Presenter {
    AccountContrant.Start.View mView;

    public StartPresenterImpl(AccountContrant.Start.View mView) {
        this.mView = mView;
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {

    }

    @Override
    public void save(String host) {
        mView.onGetStart();
    }
}

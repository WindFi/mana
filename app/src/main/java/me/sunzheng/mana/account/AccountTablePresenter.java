package me.sunzheng.mana.account;

/**
 * Created by Sun on 2017/7/31.
 */

public class AccountTablePresenter implements AccountContrant.Login.Presenter, AccountContrant.Start.Presenter {

    AccountContrant.Login.Presenter loginPresenter;
    AccountContrant.Start.Presenter startPresenter;

    public AccountTablePresenter(AccountContrant.Start.Presenter startPresenter) {
        this.startPresenter = startPresenter;
    }

    public void setLoginPresenter(AccountContrant.Login.Presenter presenter) {
        this.loginPresenter = presenter;
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {
        if (startPresenter != null)
            startPresenter.unsubscribe();
        if (loginPresenter != null)
            loginPresenter.unsubscribe();
    }

    @Override
    public void login(String userName, String passWord, boolean isRemembered) {
        loginPresenter.login(userName, passWord, isRemembered);
    }

    @Override
    public void save(String host) {
        startPresenter.save(host);
    }
}

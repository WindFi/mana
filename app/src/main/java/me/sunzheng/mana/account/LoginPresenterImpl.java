package me.sunzheng.mana.account;

import android.util.Log;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Sun on 2017/5/22.
 */

public class LoginPresenterImpl implements LoginContract.LoginPresenter {
    LoginContract.LoginView mView;
    AccountApiService.Login service;
    CompositeDisposable compositeDisposable = new CompositeDisposable();

    public LoginPresenterImpl(LoginContract.LoginView loginView, AccountApiService.Login service) {
        this.mView = loginView;
        this.service = service;
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {
        compositeDisposable.clear();
    }

    @Override
    public void login(String userName, String passWord) {
        mView.showProgressIntractor(true);
        LoginRequest request = new LoginRequest();
        request.name = userName;
        request.password = passWord;
        request.remember = true;
        Disposable disposable = service.login(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnTerminate(new Action() {
                    @Override
                    public void run() throws Exception {
                        mView.showProgressIntractor(false);
                    }
                })
                .subscribe(new Consumer<LoginResponse>() {
                    @Override
                    public void accept(LoginResponse loginSuccessResponse) throws Exception {
                        mView.showToast(loginSuccessResponse.msg);
                        mView.onLoginSuccess();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if (throwable != null)
                            Log.e("login e", throwable.getLocalizedMessage());
                        mView.showToast(throwable.getLocalizedMessage());
                    }
                });
        compositeDisposable.add(disposable);
    }
}

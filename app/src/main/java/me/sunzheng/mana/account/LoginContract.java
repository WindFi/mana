package me.sunzheng.mana.account;

import me.sunzheng.mana.IPresenter;
import me.sunzheng.mana.IView;

/**
 * Created by Sun on 2017/5/22.
 */

public interface LoginContract {
    interface LoginView extends IView<LoginPresenter>{
        void showToast(String message);
        void showProgressIntractor(boolean active);
        void onLoginSuccess();
    }
    interface LoginPresenter extends IPresenter{
        void login(String userName,String passWord);
    }
}

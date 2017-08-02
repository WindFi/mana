package me.sunzheng.mana.account;

import me.sunzheng.mana.utils.IPresenter;
import me.sunzheng.mana.utils.IView;

/**
 * Created by Sun on 2017/7/31.
 */

public interface AccountContrant {
    /**
     * Created by Sun on 2017/5/22.
     */

    interface Login {
        interface View extends IView<Presenter> {
            void showToast(String message);

            void showProgressIntractor(boolean active);

            void onLoginSuccess();
        }

        interface Presenter extends IPresenter {
            void login(String userName, String passWord);
        }
    }

    interface Start {
        interface View extends IView<Presenter> {
            void onGetStart();
        }

        interface Presenter extends IPresenter {
            void save(String host);
        }
    }
}

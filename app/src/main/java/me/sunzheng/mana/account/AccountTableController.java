package me.sunzheng.mana.account;

import android.support.v4.app.FragmentManager;

import me.sunzheng.mana.account.config.HostFragment;
import me.sunzheng.mana.account.config.StartPresenterImpl;
import me.sunzheng.mana.account.login.LoginPresenterImpl;

/**
 * Created by Sun on 2017/7/31.
 */

public class AccountTableController {
    int replaceId;
    FragmentManager fragmentManager;
    AccountTablePresenter presenter;
    HostFragment hostDialogFragment = new HostFragment();
    LoginFragment loginFragment = LoginFragment.newInstance();

    public AccountTableController(int replaceId, FragmentManager fragmentManager) {
        this.replaceId = replaceId;
        this.fragmentManager = fragmentManager;
        presenter = new AccountTablePresenter(new StartPresenterImpl(hostDialogFragment));
        createElements();
    }

    void createElements() {
        fragmentManager.beginTransaction().replace(replaceId, hostDialogFragment).commit();
    }

    public void loginViewShow(AccountApiService.Login apiService) {
        presenter.setLoginPresenter(new LoginPresenterImpl(loginFragment, apiService));
        loginFragment.setPresenter(presenter);
        fragmentManager.beginTransaction().replace(replaceId, loginFragment).addToBackStack("host").commit();
    }
}

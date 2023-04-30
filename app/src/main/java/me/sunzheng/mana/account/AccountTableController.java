package me.sunzheng.mana.account;

import androidx.fragment.app.FragmentManager;

import me.sunzheng.mana.account.config.HostFragment;
import me.sunzheng.mana.account.login.LoginFragment;

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
//        presenter = new AccountTablePresenter(new StartPresenterImpl(hostDialogFragment));
        createElements();
    }

    void createElements() {
        fragmentManager.beginTransaction().replace(replaceId, hostDialogFragment).commit();
    }

    public void loginViewShow(AccountApiService.Login apiService) {
//        presenter.setLoginPresenter(new LoginPresenterImpl(loginFragment, apiService));
//        loginFragment.setPresenter(presenter);
//        fragmentManager.beginTransaction()
//                .setCustomAnimations(R.anim.slide_in_right, android.R.anim.slide_out_right, R.anim.slide_in_right, android.R.anim.slide_out_right)
//                .addToBackStack("host")
//                .replace(replaceId, loginFragment)
//                .commit();
    }
}

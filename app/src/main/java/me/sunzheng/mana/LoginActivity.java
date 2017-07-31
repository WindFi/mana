package me.sunzheng.mana;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import me.sunzheng.mana.account.AccountApiService;
import me.sunzheng.mana.account.AccountTableController;
import me.sunzheng.mana.account.LoginFragment;
import me.sunzheng.mana.account.config.HostFragment;
import me.sunzheng.mana.utils.App;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements HostFragment.OnButtonClickListener, LoginFragment.OnFragmentInteractionListener {
    private final static String TAG = LoginActivity.class.getSimpleName();
    AccountTableController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        controller = new AccountTableController(R.id.replace, getSupportFragmentManager());
    }


    @Override
    protected void onDestroy() {
        controller = null;
        super.onDestroy();
    }

    @Override
    public void onSave() {
        ((App) getApplicationContext()).initClient();
        controller.loginViewShow(((App) getApplicationContext()).getRetrofit().create(AccountApiService.Login.class));
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}


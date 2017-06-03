package me.sunzheng.mana;

/**
 * Created by Sun on 2017/5/21.
 */

public interface PreferenceManager {
    interface Global{
        String STR_SP_NAME="global";
        String STR_KEY_HOST="host";
        String INT_KEY_DEFAULT_TIMEOUT="timeout";
        String BOOL_IS_REMEMBERD="rememberd";

        String STR_USERNAME="username";
        String STR_PASSWORD="password";
    }
}

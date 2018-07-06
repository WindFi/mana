package me.sunzheng.mana.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

public class LanguageSwitchUtils {
    public static String switchLanguageToJa(Context context, String jaString, String cnString) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isJaFirst = sharedPreferences.getBoolean(context.getString(me.sunzheng.mana.utils.PreferenceManager.Global.RES_JA_FIRST_BOOL), false);
        return isJaFirst ? jaString : cnString;
    }
}

package me.sunzheng.mana.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import me.sunzheng.mana.R;

public class LanguageSwitchUtils {
    public static String switchLanguageToJa(@NonNull Context context, String jaString, String cnString) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isJaFirst = sharedPreferences.getBoolean(context.getString(R.string.pref_key_ja_first_bool), false);
        return isJaFirst ? jaString : cnString;
    }
}

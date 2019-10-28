package me.sunzheng.mana.utils;

import androidx.core.util.PatternsCompat;

public class HostUtil {
    public static String makeUp(String host, String url) {
        if (PatternsCompat.WEB_URL.matcher(url).matches())
            return url;
        else {
            return host + url;
        }
    }
}

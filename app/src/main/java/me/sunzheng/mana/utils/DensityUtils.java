package me.sunzheng.mana.utils;

import android.content.res.Resources;

/**
 * Created by Sun on 2018/1/22.
 */

public class DensityUtils {
    public static float dip2px(float dip) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return dip * scale;
    }

    public static float px2Dip(float px) {
        final float scale = Resources.getSystem().getDisplayMetrics().density;
        return px / scale;
    }
}

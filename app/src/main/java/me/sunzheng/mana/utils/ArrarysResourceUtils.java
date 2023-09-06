package me.sunzheng.mana.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import me.sunzheng.mana.R;

public class ArrarysResourceUtils {
    public static String dayInWeek(@NonNull Context context, int day) {
        String[] days = context.getResources().getStringArray(R.array.weeks);
        return days[day % days.length];
    }
}

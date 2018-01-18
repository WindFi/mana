package me.sunzheng.mana.settings;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.util.AttributeSet;

/**
 * Created by Sun on 2018/1/18.
 */

public class PlaybackPreference extends Preference {
    private boolean isAutoPlay;

    public PlaybackPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PlaybackPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PlaybackPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlaybackPreference(Context context) {
        super(context);
    }
}

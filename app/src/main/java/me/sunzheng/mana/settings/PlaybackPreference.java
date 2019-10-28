package me.sunzheng.mana.settings;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.Preference;

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

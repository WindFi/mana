package me.sunzheng.mana.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.SeekBar;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.util.Util;

import java.util.Formatter;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.AppCompatTextView;
import me.sunzheng.mana.R;

/**
 * Created by Sun on 2017/12/27.
 * Controller for ExoPlayer will support {@link android.support.v4.media.session.MediaControllerCompat} in future
 */

public class ExoPlaybackControlView extends FrameLayout {
    SimpleExoPlayer player;
    AppCompatSeekBar seekBar;
    AppCompatTextView mPositionTextView, mDurationTextView;
    StringBuilder formatBuilder;
    Formatter formatter;
    View mPrev, mPlay, mNext, mPause;
    ComponentListener componentListener;

    public ExoPlaybackControlView(@NonNull Context context) {
        super(context);
    }

    public ExoPlaybackControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExoPlaybackControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) {

            return;
        }
        mPause = findViewById(R.id.exo_pause);
        mPlay = findViewById(R.id.exo_play);
        mPrev = findViewById(R.id.exo_prev);
        mNext = findViewById(R.id.exo_next);
        mPositionTextView = findViewById(R.id.exo_position);
        mDurationTextView = findViewById(R.id.exo_duration);
        seekBar = findViewById(R.id.exo_seekbar);
        formatter = new Formatter(formatBuilder, Locale.getDefault());
        componentListener = new ComponentListener();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    seekTo((long) progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void setPlayer(SimpleExoPlayer player) {
        this.player = player;
        player.addListener(componentListener);
    }

    public void seekTo(long position) {
        player.seekTo(position);
    }

    private void updateProgress() {
        seekBar.setProgress((int) player.getCurrentPosition());
        mPositionTextView.setText(Util.getStringForTime(formatBuilder, formatter, player.getCurrentPosition()));
        mDurationTextView.setText(Util.getStringForTime(formatBuilder, formatter, player.getDuration()));
    }

    private final class ComponentListener extends Player.DefaultEventListener {
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest) {
            super.onTimelineChanged(timeline, manifest);
            updateProgress();
        }

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            super.onPlayerStateChanged(playWhenReady, playbackState);
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            super.onRepeatModeChanged(repeatMode);
        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            super.onShuffleModeEnabledChanged(shuffleModeEnabled);
        }

        @Override
        public void onPositionDiscontinuity(int reason) {
            super.onPositionDiscontinuity(reason);
        }
    }
}

package me.sunzheng.mana.widget;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.google.android.exoplayer2.SimpleExoPlayer;

import me.sunzheng.mana.R;

/**
 * Created by Sun on 2017/12/27.
 */

public class ExoPlayerView extends FrameLayout {
    SimpleExoPlayer player;
    MediaControllerCompat controllerCompat;
    Handler mHandler = new Handler();
    SurfaceView surfaceView;
    ExoPlaybackControlView controlView;
    ControllerVisibityListener controllerListener;

    public ExoPlayerView(@NonNull Context context) {
        super(context);
    }

    public ExoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (isInEditMode()) {
            player = null;
            surfaceView = null;
            return;
        }
        surfaceView = findViewById(R.id.exo_surfaceview);
        controlView = findViewById(R.id.exo_controller);
    }

    public void setPlayer(SimpleExoPlayer player) {
        player.setVideoSurfaceView(surfaceView);
    }

    public void setControllerVisibityListener(ControllerVisibityListener listener) {
        this.controllerListener = listener;
    }

    private void showPannels() {
        // TODO: 2017/12/27  show all pannels
        postRunnable(new Runnable() {
            @Override
            public void run() {
                showControlView();
                if (controllerListener != null)
                    controllerListener.onVisibityChange(View.VISIBLE);
            }
        });
    }

    private void hidePannels() {
        // TODO: 2017/12/27  hide all pannels
        postRunnable(new Runnable() {
            @Override
            public void run() {
                hideControlView();
                if (controllerListener != null)
                    controllerListener.onVisibityChange(View.GONE);
            }
        });
    }

    private void showControlView() {
        // TODO: 2017/12/27 show controlview 
    }

    private void hideControlView() {
        // TODO: 2017/12/27 hide controlview
    }

    private void postRunnable(Runnable r) {
        mHandler.postDelayed(r, 3000);
    }

    interface ControllerVisibityListener {
        void onVisibityChange(int visibity);
    }
}

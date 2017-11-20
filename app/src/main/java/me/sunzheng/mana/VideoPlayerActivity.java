package me.sunzheng.mana;

import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.EventLog;
import android.view.View;
import android.view.WindowManager;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.episode.service.PlayService;
import me.sunzheng.mana.home.episode.service.RemotePlayer;
import me.sunzheng.mana.home.episode.wrapper.EpisodeWrapper;
import me.sunzheng.mana.utils.App;

// TODO: 2017/6/1  VideoPlayer
public class VideoPlayerActivity extends Activity {
    SimpleExoPlayerView playerView;
    SimpleExoPlayer player;

    HomeApiService.Episode service;
    Handler mHandler = new Handler();
    EventLog eventLogger;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    Disposable loggerDisposable;
    RemotePlayer remote;
    AudioManager audioManager;
    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (player != null && player.getPlayWhenReady())
                    playerPlay();
            }
        }
    };
    IntentFilter intentFilter = new IntentFilter();
    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            remote = (RemotePlayer) service;
            loopAndStartPlay();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            remote = null;
        }
    };

    Observable timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.hide();
        initPlayer();
        service = ((App) getApplicationContext()).getRetrofit().create(HomeApiService.Episode.class);
        Intent serviceIntent = new Intent(this, PlayService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        initAudioManager();

        compositeDisposable = new CompositeDisposable();
    }

    private void initAudioManager() {
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(broadcastReceiver, intentFilter);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    // Permanent loss of audio focus
                    // Pause playback immediately
//                    mediaController.getTransportControls().pause();
                    // Wait 30 seconds before stopping playback
//                    mHandler.postDelayed(mDelayedStopRunnable,
//                            TimeUnit.SECONDS.toMillis(30));
                    playerPause();
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    // Pause playback
                    playerPause();
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                    // Lower the volume, keep playing
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    // Your app has been granted audio focus again
                    // Raise volume to normal, restart playback if necessary
                    playerPlay();
                }
            }
        };
        audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    private void initPlayer() {
        playerView = findViewById(R.id.player);
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        playerView.setPlayer(player);
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
        player.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {

            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            }

            @Override
            public void onLoadingChanged(boolean isLoading) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (!playWhenReady)
                    return;
                switch (playbackState) {
                    case ExoPlayer.STATE_ENDED:
//                        loopAndStartPlay();
                        intervalDispose();
                        logWatchProgress();
                        finish();
                        break;
                    case ExoPlayer.STATE_BUFFERING:
                        break;
                    case ExoPlayer.STATE_IDLE:
                        break;
                    case ExoPlayer.STATE_READY:
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {

            }

            @Override
            public void onPositionDiscontinuity() {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
            }
        });
    }

    private void play(Uri uri, long lastWatchPosition) {
        DefaultBandwidthMeter mDefaultBandwidthMeter = new DefaultBandwidthMeter();
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(VideoPlayerActivity.this, Util.getUserAgent(VideoPlayerActivity.this, getPackageName()), mDefaultBandwidthMeter);
        ExtractorsFactory extractorFactory = new DefaultExtractorsFactory();
        MediaSource videoSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorFactory, null, null);
        player.prepare(videoSource);
        player.seekTo(lastWatchPosition);
        playerPlay();
    }

    @Override
    protected void onStop() {
        playerPause();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        logWatchProgress();
        if (player != null)
            player.release();
        if (compositeDisposable != null)
            compositeDisposable.clear();
        unbindService(serviceConnection);
        if (broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        super.onDestroy();
    }
    private void playerPlay() {
        if (player != null && !player.getPlayWhenReady())
            player.setPlayWhenReady(true);
        intervalLog();
    }

    private void intervalLog() {
        loggerDisposable = Observable.interval(5000, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        logWatchProgress();
                    }
                });
    }

    private void intervalDispose() {
        if (loggerDisposable != null) {
            if (!loggerDisposable.isDisposed())
                loggerDisposable.dispose();
            loggerDisposable = null;
        }
    }

    private void logWatchProgress() {
        remote.logWatchProcess(player.getCurrentPosition(), player.getDuration());
    }

    private void playerPause() {
        if (player != null && player.getPlayWhenReady())
            player.setPlayWhenReady(false);
        intervalDispose();
        logWatchProgress();
    }

    private void loopAndStartPlay() {
        final PlayService.PlayItem item = remote.queryAllPlayList().get(remote.getPosition());
        if (!TextUtils.isEmpty(item.getId())) {
            Disposable disposable = service.getEpisode(item.getId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<EpisodeWrapper>() {
                        @Override
                        public void accept(EpisodeWrapper episodeWrapper) throws Exception {
                            String uri = episodeWrapper.getVideoFiles().get(0).getUrl();
                            play(Uri.parse(uri), item.getLastWatchPosition());
                        }
                    });
            compositeDisposable.add(disposable);
        } else {
            finish();
        }
    }
}

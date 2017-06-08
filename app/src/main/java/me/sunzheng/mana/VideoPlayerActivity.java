package me.sunzheng.mana;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.EventLog;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.episode.wrapper.EpisodeWrapper;

// TODO: 2017/6/1  VideoPlayer
public class VideoPlayerActivity extends Activity {
    public final static String ARGS_URI_STR = "uri";
    SimpleExoPlayerView playerView;
    SimpleExoPlayer player;
    Handler mHandler = new Handler();
    EventLog eventLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);
        if (savedInstanceState == null)
            savedInstanceState = getIntent().getExtras();
        if (savedInstanceState == null) {
            Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
            finish();
        }
        View decorView = getWindow().getDecorView();
// Hide the status bar.
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
//        keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        ActionBar actionBar = getActionBar();
        if (actionBar != null)
            actionBar.hide();
        playerView = (SimpleExoPlayerView) findViewById(R.id.player);
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(this, trackSelector);
        playerView.setPlayer(player);
        HomeApiService.Episode service = ((App) getApplicationContext()).getRetrofit().create(HomeApiService.Episode.class);
        final String host = getSharedPreferences(PreferenceManager.Global.STR_SP_NAME, Context.MODE_PRIVATE).getString(PreferenceManager.Global.STR_KEY_HOST, "");
        service.getEpisode(savedInstanceState.getString(ARGS_URI_STR))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<EpisodeWrapper>() {
                    @Override
                    public void accept(EpisodeWrapper episodeWrapper) throws Exception {
                        Uri uri = Uri.parse(host + episodeWrapper.getVideoFiles().get(0).getUrl());
                        DefaultBandwidthMeter mDefaultBandwidthMeter = new DefaultBandwidthMeter();
                        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(VideoPlayerActivity.this, Util.getUserAgent(VideoPlayerActivity.this, getPackageName()), mDefaultBandwidthMeter);
                        ExtractorsFactory extractorFactory = new DefaultExtractorsFactory();
                        MediaSource videoSource = new ExtractorMediaSource(uri, dataSourceFactory, extractorFactory, null, null);
                        player.setPlayWhenReady(true);
                        player.prepare(videoSource);
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        player.setPlayWhenReady(false);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        player.release();
        super.onDestroy();
    }
}

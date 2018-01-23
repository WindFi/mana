package me.sunzheng.mana;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.util.Util;

import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import me.sunzheng.mana.core.Episode;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.episode.EpisodePresenterImpl;
import me.sunzheng.mana.home.episode.LocalDataRepository;
import me.sunzheng.mana.utils.App;


/**
 * version 2
 * Created by Sun on 2017/12/14.
 */

public class VideoPlayActivity extends AppCompatActivity implements HomeContract.VideoPlayer.View {
    public final static String TAG = VideoPlayActivity.class.getSimpleName();
    public final static String STR_CURRINT_INT = "current";
    public final static String STR_ITEMS_PARCEL = "items";
    public final static long DEFAULT_HIDE_TIME = 500;
    SimpleExoPlayerView playerView;
    Toolbar toolbar;
    ListView mListView;
    boolean isResume = false, isAudioFouced = false, isControlViewVisibile;

    ViewGroup progressViewGroup;
    AppCompatTextView textViewPosition, textViewDuration;

    StringBuilder formatBuilder = new StringBuilder();
    Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());
    HomeContract.VideoPlayer.Presenter presenter;
    Handler mHnadler = new Handler();
    Runnable hideProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (progressViewGroup == null || progressViewGroup.getVisibility() != View.VISIBLE) {
                return;
            }
            progressViewGroup.setVisibility(View.GONE);
        }
    };
    Runnable hideListViewRunnable = new Runnable() {
        @Override
        public void run() {
            if (mListView == null || mListView.getVisibility() != View.VISIBLE)
                return;
            mListView.setVisibility(View.GONE);
        }
    };
    AudioManager audioManager;
    AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (presenter != null && presenter.getPlayer().getPlayWhenReady()) {
                    playerPlay();
                }
            } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                if (intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false))
                    return;
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo == null)
                    return;
                switch (activeNetworkInfo.getType()) {
                    case ConnectivityManager.TYPE_MOBILE:
                        if (activeNetworkInfo.isConnected())
//                            showAttendtionDialogAndPause();
                            break;
                    default:
                        break;
                }
            }
        }
    };

    public static Intent newInstance(Context context, int position, List<Episode> items) {
        Intent intent = new Intent(context, VideoPlayActivity.class);
        Bundle extras = new Bundle();
        extras.putInt(STR_CURRINT_INT, position);
        extras.putParcelableArray(STR_ITEMS_PARCEL, parseMediaDescriptionFromEpisode(items));
        intent.putExtras(extras);
        return intent;
    }

    private static MediaDescriptionCompat parseMediaDescriptionFromEpisode(Episode episode) {
//        holder.itemView.getContext().getString(R.string.episode_template, item.getEpisodeNo() + "")
        return new MediaDescriptionCompat.Builder().setTitle(episode.getEpisodeNo() + "")
                .setDescription(episode.getNameCn())
                .setIconUri(Uri.parse(episode.getThumbnailImage() == null ? episode.getThumbnail() : episode.getThumbnailImage().url))
                .setMediaId(episode.getId()).build();
    }

    private static MediaDescriptionCompat[] parseMediaDescriptionFromEpisode(List<Episode> episodes) {
        MediaDescriptionCompat[] arrs = new MediaDescriptionCompat[episodes.size()];
        Collections.sort(episodes, new Comparator<Episode>() {
            @Override
            public int compare(Episode o1, Episode o2) {
                return o1.getEpisodeNo() - o2.getEpisodeNo();
            }
        });
        for (int i = 0; i < arrs.length; i++) {
            arrs[i] = parseMediaDescriptionFromEpisode(episodes.get(i));
        }
        return arrs;
    }

    private void initAudioManager() {
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    isAudioFouced = false;
                    playerPause();
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    // Pause playback
                    isAudioFouced = false;
                    playerPause();
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {

                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    isAudioFouced = true;
                    playerPlay();
                }
            }
        };
        audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    @Override
    public void setPlayItemChecked(int position, boolean isChecked) {
        if (mListView == null || position >= mListView.getCount())
            return;
        mListView.setItemChecked(position, isChecked);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        savedInstanceState = savedInstanceState == null ? getIntent().getExtras() : savedInstanceState;
        int current = savedInstanceState.getInt(STR_CURRINT_INT, 0);

        Parcelable[] parcelableArray = savedInstanceState.getParcelableArray(STR_ITEMS_PARCEL);
        MediaDescriptionCompat[] items = convertFromParcelable(parcelableArray);

        playerView = (SimpleExoPlayerView) findViewById(R.id.player);
        mListView = (ListView) findViewById(R.id.list);

        progressViewGroup = (ViewGroup) findViewById(R.id.progress_viewgroup);
        textViewDuration = (AppCompatTextView) findViewById(R.id.exo_duration_textview);
        textViewPosition = (AppCompatTextView) findViewById(R.id.exo_position_textview);

        final GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(this, new PresenterGestureDetector());

        presenter = new EpisodePresenterImpl(this, ((App) getApplication()).getRetrofit().create(HomeApiService.Episode.class), ((App) getApplication()).getRetrofit().create(HomeApiService.Bangumi.class), new LocalDataRepository(items, current));
        playerView.setControllerVisibilityListener(new PlaybackControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {
                isControlViewVisibile = visibility == View.VISIBLE;
                if (isControlViewVisibile) {
                    showControllView();
                } else {
                    hideControlView();
                }
            }
        });
        playerView.setPlayer(presenter.getPlayer());
        playerView.setKeepScreenOn(true);
        playerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetectorCompat.onTouchEvent(event);
            }
        });
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                presenter.tryPlayItem(((ListView) parent).getCheckedItemPosition());
                hideEpisodeListView();
            }

        });
        presenter.tryPlayItem(current);
//        mListView.performItemClick(mListView.getAdapter().getView(current,null,null),current,0);
        initAudioManager();
        presenter.getPlayer().addListener(new Player.DefaultEventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                super.onPlayerStateChanged(playWhenReady, playbackState);
                switch (playbackState) {
                    case Player.STATE_ENDED:
                        if (isAutoPlay() && !isListEnd())
                            presenter.tryPlayItem(mListView.getCheckedItemPosition() + 1);
                        else
                            finish();
                        break;
                    default:

                }
            }
        });
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    private boolean isAutoPlay() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("isAutoplay", false);
    }

    private boolean isListEnd() {
        return mListView.getCheckedItemPosition() >= mListView.getCount() - 1;
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public void setPresenter(HomeContract.VideoPlayer.Presenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void setAdapter(BaseAdapter adapter) {
        mListView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return;
        isResume = true;
        playerPlay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            return;
        isResume = true;
        playerPlay();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return;
        isResume = false;
        playerPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            return;
        isResume = false;
        playerPause();
    }

    void hideControlView() {
        getSupportActionBar().hide();
        playerView.hideController();
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    void showControllView() {
        getSupportActionBar().show();
        playerView.showController();
    }

    void showEpisodeListView() {
        mListView.setVisibility(View.VISIBLE);
        hideControlView();
    }

    boolean consumeEpisodeListView() {
        if (mListView.getVisibility() == View.VISIBLE) {
            hideEpisodeListView();
            return true;
        }
        return false;
    }

    void hideEpisodeListView() {
        hideEpisodeListView(0);
    }

    void hideEpisodeListView(long delayMillisecond) {
        mHnadler.postDelayed(hideListViewRunnable, delayMillisecond);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.video_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        long itemId = item.getItemId();
        if (itemId == R.id.action_list) {
            showEpisodeListView();
            return true;
        } else if (itemId == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!consumeEpisodeListView())
            super.onBackPressed();
    }


    private MediaDescriptionCompat[] convertFromParcelable(Parcelable[] parcelables) {
        MediaDescriptionCompat[] _items = new MediaDescriptionCompat[parcelables.length];
        for (int i = 0; i < _items.length; i++) {
            _items[i] = (MediaDescriptionCompat) parcelables[i];
        }
        return _items;
    }

    void setVolume(float detaVal) {
        int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVol += detaVal;
        currentVol = currentVol > maxVol ? maxVol : currentVol;
        currentVol = currentVol > -1 ? currentVol : 0;
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVol, 0);
        showVolumeVal(currentVol);
    }

    void setCurrentPosition(float detaVal) {
        long position = playerView.getPlayer().getCurrentPosition() + (long) detaVal * 5000;
        playerView.getPlayer().seekTo(position);
        showProgressDetaVal(0);
    }
    void setBrightness(float detaVal) {
        float per = detaVal / 16 * 255.0f;
        float currentBrightness = getWindow().getAttributes().screenBrightness * 255.f;
        currentBrightness += per;
        currentBrightness = currentBrightness < 256 ? currentBrightness : 255;
        currentBrightness = currentBrightness > -1 ? currentBrightness : 0;

        WindowManager.LayoutParams layoutpars = getWindow().getAttributes();
        layoutpars.screenBrightness = currentBrightness / 255.0f;
        getWindow().setAttributes(layoutpars);
        showBrightnessVal((int) currentBrightness);
    }

    @Override
    public void setMediaTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void showVolumeVal(int val) {
        Toast.makeText(this, "vol:" + val, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void showProgressDetaVal(int detaVal) {
        mHnadler.removeCallbacks(hideProgressRunnable);
        progressViewGroup.setVisibility(View.VISIBLE);
        textViewPosition.setText(getStringForTime(playerView.getPlayer().getCurrentPosition()));
        textViewDuration.setText(getStringForTime(playerView.getPlayer().getDuration()));
        mHnadler.postDelayed(hideProgressRunnable, DEFAULT_HIDE_TIME);
    }

    @Override
    public void showBrightnessVal(int val) {
        Log.i(TAG, "not implements");
        Toast.makeText(this, "Brightness:" + val, Toast.LENGTH_SHORT).show();
    }

    void playerPlay() {
        if (isPlayerFocusedAndResume())
            presenter.play();
    }

    void playerPause() {
        if (presenter != null && presenter.getPlayer().getPlayWhenReady())
            presenter.pause();
    }

    boolean isPlayerFocusedAndResume() {
        return isResume && isAudioFouced;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);
        if (presenter != null) {
            presenter.release();
            presenter.unsubscribe();
        }
        // TODO: 2018/1/16 Need version compation for api 26
        audioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    private String getStringForTime(long timeMs) {
        return Util.getStringForTime(formatBuilder, formatter, timeMs);
    }

    public final class PresenterGestureDetector extends GestureDetector.SimpleOnGestureListener {
        final static float MEASURE_LENGTH = 72.0f;
        //max 16
        final static int SPLITE_UNIT = 1;
        boolean isVertical, isLeft;
        volatile float sourceX, sourceY;
        boolean isScrolling;

        @Override
        public boolean onDown(MotionEvent e) {
            isScrolling = false;
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!isScrolling) {
                isVertical = Math.abs(distanceX) < Math.abs(distanceY);
                isScrolling = true;
                sourceX = e1.getX();
                sourceY = e1.getY();
                if (isVertical) {
                    Point p = new Point();
                    getWindowManager().getDefaultDisplay().getSize(p);
                    isLeft = e1.getX() < p.x / 2;
                }
            } else {
                if (!isVertical) {
                    // TODO: 2018/1/19 seek to
                    float unit = (int) ((e2.getX() - sourceX) / MEASURE_LENGTH);
                    if (Math.abs(unit) > 0) {
                        sourceX = e2.getX();
                    }
                    setCurrentPosition(unit);
                } else {
                    float unit = (int) ((sourceY - e2.getY()) / MEASURE_LENGTH);
                    if (Math.abs(unit) > 0) {
                        sourceY = e2.getY();
                    }
                    if (isLeft) {
                        // 2018/1/20  2018/1/19 brightness
                        setBrightness(unit);
                    } else {
                        // 2018/1/19 Vol
                        setVolume(unit);
                    }
                }
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (isVertical) {
                // TODO: 2018/1/19  seek to
            }
            sourceY = 0;
            sourceX = 0;
            isScrolling = false;
            isVertical = false;
            return true;
        }


        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.i(TAG, "onDoubleTap:" + System.currentTimeMillis());
            return presenter.doubleClick();
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.i(TAG, "onSingleTapConfirmed" + System.currentTimeMillis());
            boolean flag = consumeEpisodeListView();
            if (!flag && !isControlViewVisibile) {
                showControllView();
            } else {
                hideControlView();
            }
            return flag;
        }
    }
}

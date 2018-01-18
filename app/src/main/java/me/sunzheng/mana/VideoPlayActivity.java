package me.sunzheng.mana;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.sunzheng.mana.core.Episode;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.episode.EpisodePresenterImpl;
import me.sunzheng.mana.home.episode.LocalDataRepository;
import me.sunzheng.mana.utils.App;
import me.sunzheng.mana.utils.PreferenceManager;


/**
 * version 2
 * Created by Sun on 2017/12/14.
 */

public class VideoPlayActivity extends AppCompatActivity implements HomeContract.VideoPlayer.View {
    public final static String TAG = VideoPlayActivity.class.getSimpleName();
    public final static String STR_CURRINT_INT = "current";
    public final static String STR_ITEMS_PARCEL = "items";
    SimpleExoPlayerView playerView;
    Toolbar toolbar;
    ListView mListView;
    HomeContract.VideoPlayer.Presenter presenter;
    Handler mHnadler = new Handler();

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
                if (presenter != null)
                    presenter.play();
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
                    presenter.pause();
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    // Pause playback
                    presenter.pause();
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {

                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    presenter.play();
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
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        savedInstanceState = savedInstanceState == null ? getIntent().getExtras() : savedInstanceState;
        int current = savedInstanceState.getInt(STR_CURRINT_INT, 0);

        Parcelable[] parcelableArray = savedInstanceState.getParcelableArray(STR_ITEMS_PARCEL);
        MediaDescriptionCompat[] items = convertFromParcelable(parcelableArray);

        playerView = findViewById(R.id.player);
        mListView = findViewById(R.id.list);

        final GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(this, new PresenterGestureDetector());

        presenter = new EpisodePresenterImpl(this, ((App) getApplication()).getRetrofit().create(HomeApiService.Episode.class), ((App) getApplication()).getRetrofit().create(HomeApiService.Bangumi.class), new LocalDataRepository(items, current));
        playerView.setControllerVisibilityListener(new PlaybackControlView.VisibilityListener() {
            @Override
            public void onVisibilityChange(int visibility) {
                if (visibility == View.VISIBLE) {
                    showControllView();
                } else {
                    hideControlView();
                }
            }
        });
        playerView.setPlayer(presenter.getPlayer());
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
        // TODO: 2018/1/4 choice
        presenter.tryPlayItem(current);
//        ----------test code-----------
//        mListView.performItemClick()
//        ------------------------------
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
        // TODO: 2018/1/12  auto play switcher
        SharedPreferences sharedPreferences = getSharedPreferences(PreferenceManager.PlayerPolicy.STR_SP_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(PreferenceManager.PlayerPolicy.BOOL_KEY_AUTOPLAY, false);
    }

    private boolean isListEnd() {
        return mListView.getCheckedItemPosition() < mListView.getCount() - 1;
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
        presenter.play();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            return;
        presenter.play();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return;
        presenter.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            return;
        presenter.pause();
    }

    void hideControlView() {
        getSupportActionBar().hide();
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    void showControllView() {
        getSupportActionBar().show();
        playerView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN);// hide status bar
    }

    void showEpisodeListView() {
        mListView.setVisibility(View.VISIBLE);
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

    @Override
    public void setMediaTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void showVolumeVal(int val) {
        Log.i(TAG, "not implements");
    }

    @Override
    public void showProgressDetaVal(int detaVal) {
        Log.i(TAG, "not implements");
    }

    @Override
    public void showLightVal(int val) {
        Log.i(TAG, "not implements");
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

    public final class PresenterGestureDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return consumeEpisodeListView();
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(velocityX) > Math.abs(velocityY)) {
                // TODO: 2018/1/15  seek
            } else {
                if (e1.getRawX() < 100 / 2) {
                    // TODO: 2018/1/15 light 
                } else {
                    // TODO: 2018/1/15 volume
                }
            }
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
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            Log.i(TAG, "onDoubleTapEvent" + System.currentTimeMillis());
            return false;
        }

    }
}

package me.sunzheng.mana;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.ResultReceiver;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.mediasession.DefaultPlaybackController;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueEditor;
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.DynamicConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ShuffleOrder;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;


/**
 * version 2
 * Created by Sun on 2017/12/14.
 */

public class VideoPlayActivity extends AppCompatActivity {
    MediaSessionCompat mSession;
    MediaSessionConnector connector;
    SimpleExoPlayer player;
    DynamicConcatenatingMediaSource mediaSource;
    TimelineQueueEditor.MediaSourceFactory mediaSourceFactory = new TimelineQueueEditor.MediaSourceFactory() {
        @Nullable
        @Override
        public MediaSource createMediaSource(MediaDescriptionCompat description) {
            DefaultBandwidthMeter mDefaultBandwidthMeter = new DefaultBandwidthMeter();
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(VideoPlayActivity.this, Util.getUserAgent(VideoPlayActivity.this, getPackageName()), mDefaultBandwidthMeter);
            ExtractorsFactory extractorFactory = new DefaultExtractorsFactory();
            MediaSource source = new ExtractorMediaSource(description.getMediaUri(), dataSourceFactory, extractorFactory, null, null);
            return source;
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        mSession = new MediaSessionCompat(this, "hello");
        setContentView(R.layout.activity_video_play);

        mSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mSession.setMediaButtonReceiver(null);
        PlaybackStateCompat.Builder builder = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mSession.setPlaybackState(builder.build());
        mediaSource = new DynamicConcatenatingMediaSource(new ShuffleOrder.UnshuffledShuffleOrder(5));
        connector = new MediaSessionConnector(mSession, new DefaultPlaybackController());
        connector.setPlayer(player, new PlaybackPreparerImpl());
        connector.setQueueEditor(new TimelineQueueEditor(mSession.getController(), mediaSource, new QueueAdapter(), mediaSourceFactory));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            mSession.setActive(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            mSession.setActive(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            mSession.setActive(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            mSession.setActive(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSession.release();
    }

    public final static class PlaybackPreparerImpl implements MediaSessionConnector.PlaybackPreparer {
        @Override
        public long getSupportedPrepareActions() {
            return 0;
        }

        @Override
        public void onPrepare() {

        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {

        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {

        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {

        }

        @Override
        public void onCommand(Player player, String s, Bundle bundle, ResultReceiver resultReceiver) {

        }

        @Override
        public String[] getCommands() {
            return new String[0];
        }
    }

    public final static class QueueNavigatorImpl extends TimelineQueueNavigator {

        public QueueNavigatorImpl(MediaSessionCompat mediaSession) {
            super(mediaSession);
        }

        public QueueNavigatorImpl(MediaSessionCompat mediaSession, int maxQueueSize) {
            super(mediaSession, maxQueueSize);
        }

        @Override
        public MediaDescriptionCompat getMediaDescription(int windowIndex) {
//            MediaDescriptionCompat mediaDescription = new MediaDescriptionCompat();
//            return mediaDescription;
            return null;
        }
    }

    public final static class QueueAdapter implements TimelineQueueEditor.QueueDataAdapter {
        ArrayList<MediaDescriptionCompat> mQueue;

        public QueueAdapter() {
            this(new ArrayList<MediaDescriptionCompat>(2));
        }

        public QueueAdapter(ArrayList<MediaDescriptionCompat> queue) {
            this.mQueue = mQueue;
        }

        @Override
        public MediaDescriptionCompat getMediaDescription(int position) {
            if (mQueue == null || mQueue.isEmpty() || mQueue.size() + 1 < position)
                return null;
            return mQueue.get(position);
        }

        @Override
        public void add(int position, MediaDescriptionCompat description) {
            mQueue.add(position, description);
        }

        @Override
        public void remove(int position) {
            mQueue.remove(position);
        }

        @Override
        public void move(int from, int to) {
            MediaDescriptionCompat value = mQueue.get(from);
            mQueue.remove(from);
            mQueue.add(to, value);
        }
    }
}

package me.sunzheng.mana.home.episode;

import android.net.Uri;
import android.util.Log;

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
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.core.Episode;
import me.sunzheng.mana.core.VideoFile;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.episode.wrapper.EpisodeWrapper;

/**
 * Created by Sun on 2018/1/2.
 */

public class EpisodePresenterImpl implements HomeContract.VideoPlayer.Presenter {
    public final static String TAG = EpisodePresenterImpl.class.getSimpleName();
    HomeApiService.Episode eApiService;
    HomeApiService.Bangumi bApiService;
    CompositeDisposable compositeDisposable = new CompositeDisposable();
    DataRepository dataRepository;
    HomeContract.VideoPlayer.View mView;
    SimpleExoPlayer player;
    DefaultBandwidthMeter mDefaultBandwidthMeter = new DefaultBandwidthMeter();
    DataSource.Factory dataSourceFactory;
    ExtractorsFactory extractorFactory = new DefaultExtractorsFactory();

    public EpisodePresenterImpl(HomeContract.VideoPlayer.View view, HomeApiService.Episode eApiService, HomeApiService.Bangumi bApiService, DataRepository dataRepository) {
        this.eApiService = eApiService;
        this.bApiService = bApiService;
        this.mView = view;
        this.dataRepository = dataRepository;
        init();
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void unsubscribe() {
        compositeDisposable.clear();
    }

    private void init() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(mView.getContext(), trackSelector);
        dataSourceFactory = new DefaultDataSourceFactory(mView.getContext(), Util.getUserAgent(mView.getContext(), mView.getContext().getPackageName()), mDefaultBandwidthMeter);
        mView.setAdapter(new MediaDescriptionAdapter(mView.getContext(), dataRepository));
    }

    @Override
    public void addPlayQueue(Episode episode) {

    }

    private void getQueueItem() {

    }

    private void getUri() {

    }

    private void playMediaFromEpisodeId(String episodeId) {
        Disposable disposable = eApiService.getEpisode(episodeId)
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                .subscribe(new Consumer<EpisodeWrapper>() {
                    @Override
                    public void accept(EpisodeWrapper episodeWrapper) throws Exception {
                        VideoFile videoFile = episodeWrapper.getVideoFiles().get(0);
                        MediaSource source = new ExtractorMediaSource(Uri.parse(videoFile.getUrl()), dataSourceFactory, extractorFactory, null, null);
                        player.prepare(source);
                        play();
                    }
                });
        compositeDisposable.add(disposable);
    }

    @Override
    public SimpleExoPlayer getPlayer() {
        return player;
    }

    @Override
    public void syncWatchLog(String episodeId, long position, long duration) {

    }

    @Override
    public boolean isEndOfList() {
        return false;
    }

    @Override
    public void release() {
        if (player == null)
            return;
        player.release();
    }

    @Override
    public void play() {
        if (player == null)
            return;
        player.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        if (player == null)
            return;
        player.setPlayWhenReady(false);
    }

    @Override
    public void doubleClick() {
        player.setPlayWhenReady(!player.getPlayWhenReady());
    }

    @Override
    public void tryPlayItem(int position) {
        mView.setPlayItemChecked(position, true);
        dataRepository.setCurrentPosition(position);
        playMediaFromEpisodeId(dataRepository.getItemByPosition(position).getMediaId());
    }

    @Override
    public void setVolumeVal(int val) {
        Log.i(TAG, "not implements yet");
    }

    @Override
    public void setLightVal(int val) {
        Log.i(TAG, "not implements yet");
    }

    @Override
    public void seekTo(long detaVal) {
        Log.i(TAG, "not implements yet");
    }
}

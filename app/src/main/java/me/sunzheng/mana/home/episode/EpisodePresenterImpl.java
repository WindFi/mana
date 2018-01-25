package me.sunzheng.mana.home.episode;

import android.net.Uri;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.core.Episode;
import me.sunzheng.mana.core.VideoFile;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.HomeContract;
import me.sunzheng.mana.home.bangumi.Response;
import me.sunzheng.mana.home.bangumi.wrapper.request.SynchronizeEpisodeHistoryWrapper;
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
    WatchProgressLoggerDelegator watchProgressLoggerDelegator;

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
        compositeDisposable.dispose();
        compositeDisposable.clear();
        if (watchProgressLoggerDelegator != null)
            watchProgressLoggerDelegator.recycle();
    }

    private void init() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        player = ExoPlayerFactory.newSimpleInstance(mView.getContext(), trackSelector);
        dataSourceFactory = new DefaultDataSourceFactory(mView.getContext(), Util.getUserAgent(mView.getContext(), mView.getContext().getPackageName()), mDefaultBandwidthMeter);
        mView.setEpisodeAdapter(new MediaDescriptionAdapter(mView.getContext(), dataRepository));
    }

    @Override
    public void addPlayQueue(Episode episode) {

    }

    private void getQueueItem() {

    }

    private void getUri() {

    }

    private void playMediaFromEpisodeId(final String episodeId) {
        Disposable disposable = eApiService.getEpisode(episodeId)
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                .subscribe(new Consumer<EpisodeWrapper>() {
                    @Override
                    public void accept(EpisodeWrapper episodeWrapper) throws Exception {
                        if (watchProgressLoggerDelegator != null) {
                            compositeDisposable.add(watchProgressLoggerDelegator.logWatchProgressNow());
                            watchProgressLoggerDelegator.recycle();
                        }
                        compositeDisposable.clear();
                        watchProgressLoggerDelegator = new WatchProgressLoggerDelegator(episodeWrapper.getBangumiId(), episodeWrapper.getId(), bApiService, player);
                        compositeDisposable.add(watchProgressLoggerDelegator.logWatchProgressWithInternal(5000));
                        VideoFile videoFile = episodeWrapper.getVideoFiles().get(0);

                        MediaSource source = new ExtractorMediaSource(Uri.parse(videoFile.getUrl()), dataSourceFactory, extractorFactory, null, null);
                        player.prepare(source);
                        play();
                        mView.setMediaTitle(episodeWrapper.getEpisodeNo() + "");
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.i(TAG, throwable.getLocalizedMessage());
                    }
                });
        compositeDisposable.add(disposable);
    }

    @Override
    public SimpleExoPlayer getPlayer() {
        return player;
    }

    @Override
    public boolean isEndOfList() {
        return true;
    }

    @Override
    public void release() {
        if (player == null)
            return;
        player.release();
        if (watchProgressLoggerDelegator != null) {
            compositeDisposable.add(watchProgressLoggerDelegator.logWatchProgressNow());
            watchProgressLoggerDelegator.recycle();
        }
    }

    @Override
    public void play() {
        if (player == null)
            return;
        player.setPlayWhenReady(true);
        if (watchProgressLoggerDelegator != null) {
            watchProgressLoggerDelegator.stop();
            compositeDisposable.add(watchProgressLoggerDelegator.logWatchProgressWithInternal(5000));
        }
    }

    @Override
    public void pause() {
        if (player == null)
            return;
        player.setPlayWhenReady(false);
        if (watchProgressLoggerDelegator != null) {
            watchProgressLoggerDelegator.stop();
            compositeDisposable.add(watchProgressLoggerDelegator.logWatchProgressNow());
        }
    }

    @Override
    public boolean doubleClick() {
        player.setPlayWhenReady(!player.getPlayWhenReady());
        return true;
    }

    @Override
    public void tryPlayItem(int position) {
        mView.setPlayItemChecked(position, true);
        dataRepository.setCurrentPosition(position);
        playMediaFromEpisodeId(dataRepository.getItemByPosition(position).getMediaId());
    }

    @Override
    public void seekTo(float detaVal) {
        long position = player.getCurrentPosition() + (long) detaVal * 5000;
        player.seekTo(position);
        mView.showProgressDetaVal(0);
    }

    final class WatchProgressLoggerDelegator {
        String bangumiId;
        String episodeId;
        HomeApiService.Bangumi apiService;
        Player player;

        public WatchProgressLoggerDelegator(String bangumiId, String episodeId, HomeApiService.Bangumi apiService, Player player) {
            this.bangumiId = bangumiId;
            this.episodeId = episodeId;
            this.apiService = apiService;
            this.player = player;
        }

        public Disposable logWatchProgressNow() {
            Disposable disposable = createRecordObservable().subscribe(new Consumer<Response>() {
                @Override
                public void accept(Response response) throws Exception {

                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) throws Exception {
                    Log.i(TAG, throwable.getLocalizedMessage());
                }
            });
            return disposable;
        }

        public Disposable logWatchProgressWithInternal(long interval) {
            Disposable disposable = Observable.interval(interval, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .flatMap(new Function<Long, Observable<Response>>() {
                        @Override
                        public Observable<Response> apply(Long aLong) throws Exception {
                            return createRecordObservable();
                        }
                    }).subscribe(new Consumer<Response>() {
                        @Override
                        public void accept(Response response) throws Exception {
                            Log.i(TAG, response.message);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            Log.i(TAG, throwable.getLocalizedMessage());
                        }
                    });
            return disposable;
        }

        private Observable<Response> createRecordObservable() {
            SynchronizeEpisodeHistoryWrapper request = new SynchronizeEpisodeHistoryWrapper();
            Record item = new Record();
            item.setBangumiId(bangumiId);
            item.setEpisodeId(episodeId);
            item.setLastWatchPosition(player.getCurrentPosition());
            item.setLastWatchTime(System.currentTimeMillis());
            item.setPercentage((float) player.getCurrentPosition() / (float) player.getDuration());
            item.setIsFinished(player.getCurrentPosition() >= player.getDuration());
            List<Record> list = new ArrayList<>();
            list.add(item);
            request.setItem(list);
            return apiService.synchronizeEpisodeHistory(request).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        }

        public void stop() {
            compositeDisposable.clear();
        }

        public void recycle() {
            stop();
            apiService = null;
            player = null;
        }
    }
}

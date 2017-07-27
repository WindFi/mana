package me.sunzheng.mana.home.bangumi.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import java.util.Stack;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.home.HomeApiService;
import me.sunzheng.mana.home.episode.wrapper.EpisodeWrapper;
import me.sunzheng.mana.utils.App;

public class PlayService extends Service {
    public final static String ARGS_URIS_STR = "uris";
    HomeApiService.Episode service;
    Stack<String> playList;
    RemotePlayer playerProxy;

    // TODO: 2017/7/25  implement playlist
    public PlayService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        playList = new Stack<>();
        service = ((App) getApplicationContext()).getRetrofit().create(HomeApiService.Episode.class);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        String[] ids = bundle.getStringArray(ARGS_URIS_STR);

        if (!playList.isEmpty())
            playList.clear();
        for (int i = ids.length - 1; i > -1; i--) {
            playList.push(ids[i]);
        }
        loopAndStartPlay();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void loopAndStartPlay() {
        if (playList.isEmpty()) {
            playerProxy.exit("finish");
            return;
        }
        String itemId = playList.pop();
        service.getEpisode(itemId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<EpisodeWrapper>() {
                    @Override
                    public void accept(EpisodeWrapper episodeWrapper) throws Exception {
                        Uri uri = Uri.parse(episodeWrapper.getVideoFiles().get(0).getUrl());
                        if (playerProxy == null) {
                            // TODO: 2017/7/26
                            return;
                        }
                        playerProxy.play(uri);
                    }
                });
    }
}

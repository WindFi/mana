package me.sunzheng.mana.home.episode.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import java.util.Stack;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import me.sunzheng.mana.VideoPlayerActivity;
import me.sunzheng.mana.home.episode.Record;

public class PlayService extends Service {
    public final static String ARGS_ITEMS_STR = "items";
    public final static String ACTION_FINISH_STR = "finish";
    public final static String ACTION_FINISH_PARCEL = "process";
    Stack<String> playList;
    RemotePlayer playerProxy = new RemotePlayerStub();

    // TODO: 2017/7/25  implement playlist
    public PlayService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        playList = new Stack<>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return playerProxy;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle bundle = intent.getExtras();
        String[] ids = bundle.getStringArray(ARGS_ITEMS_STR);
        if (!playList.isEmpty())
            playList.clear();
        for (int i = ids.length - 1; i > -1; i--) {
            playList.push(ids[i]);
        }
        Observable.just("")
                .delay(300, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(String value) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {
                Intent playerIntent = new Intent(PlayService.this, VideoPlayerActivity.class);
                startActivity(playerIntent);
            }
        });

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    class RemotePlayerStub extends Binder implements RemotePlayer {

        @Override
        public void logWatchProcess(Record record) {

        }

        @Override
        public String pop() {
            return playList.pop();
        }
    }
}

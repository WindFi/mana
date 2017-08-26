package me.sunzheng.mana.home.episode.service;

import android.os.IBinder;

import java.util.List;

import me.sunzheng.mana.home.episode.Record;

/**
 * Created by Sun on 2017/7/26.
 */

public interface RemotePlayer extends IBinder {

    List<PlayService.PlayItem> queryAllPlayList();

    int getPosition();

    boolean hasNext();

    PlayService.PlayItem moveToNext();

    void logWatchProcess(Record record);

    void onDisconnect();

}

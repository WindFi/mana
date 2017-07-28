package me.sunzheng.mana.home.episode.service;

import android.os.IBinder;

import me.sunzheng.mana.home.episode.Record;

/**
 * Created by Sun on 2017/7/26.
 */

public interface RemotePlayer extends IBinder {
    String pop();

    void logWatchProcess(Record record);
}

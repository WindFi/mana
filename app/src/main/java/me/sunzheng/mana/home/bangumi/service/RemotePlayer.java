package me.sunzheng.mana.home.bangumi.service;

import android.net.Uri;
import android.os.IBinder;

/**
 * Created by Sun on 2017/7/26.
 */

public interface RemotePlayer extends IBinder {
    void play(Uri uri);

    void exit(String msg);
}

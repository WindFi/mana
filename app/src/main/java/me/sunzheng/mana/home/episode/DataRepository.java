package me.sunzheng.mana.home.episode;

import android.support.v4.media.MediaDescriptionCompat;

/**
 * Created by Sun on 2018/1/9.
 */

public interface DataRepository {
    int getCurrentPosition();

    void setCurrentPosition(int position);

    MediaDescriptionCompat getItemByPosition(int position);

    int getPositionByItem(MediaDescriptionCompat item);

    int getCount();

}

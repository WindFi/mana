package me.sunzheng.mana.home.episode;

import android.support.v4.media.MediaDescriptionCompat;

/**
 * Created by Sun on 2018/1/9.
 */

public class LocalDataRepository implements DataRepository {
    MediaDescriptionCompat[] items;
    int current;

    public LocalDataRepository(MediaDescriptionCompat[] items, int current) {
        this.items = items;
        this.current = current;
    }

    @Override
    public int getCurrentPosition() {
        return current;
    }

    @Override
    public void setCurrentPosition(int position) {
        current = position;
    }

    @Override
    public MediaDescriptionCompat getItemByPosition(int position) {
        return position < items.length && position > -1 ? items[position] : null;
    }

    @Override
    public int getPositionByItem(MediaDescriptionCompat item) {
        for (int i = 0; i < items.length; i++) {
            if (item.getMediaId().equals(items[i].getMediaId()))
                return i;
        }
        return -1;
    }

    @Override
    public int getCount() {
        return items.length;
    }
}

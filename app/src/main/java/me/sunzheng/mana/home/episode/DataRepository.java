package me.sunzheng.mana.home.episode;

import android.support.v4.media.MediaDescriptionCompat;

import me.sunzheng.mana.core.VideoFile;

/**
 * Created by Sun on 2018/1/9.
 */

public interface DataRepository {
    int getCurrentPosition();

    void setCurrentPosition(int position);

    MediaDescriptionCompat getItemByPosition(int position);

    int getPositionByItem(MediaDescriptionCompat item);

    int getEpisodeCount();

    void clearVideoFiles();

    void addVideoFile(VideoFile file);

    VideoFile getVideoFileItem(int position);

    VideoFile getVideoFileItem(String label);

    int getCurrentSourcePosition();

    void setCurrentSourcePosition(int position);

    int getVideoFilesCount();

    String[] getSourceLabels();
}

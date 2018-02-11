package me.sunzheng.mana.home.episode;

import android.support.v4.media.MediaDescriptionCompat;

import java.util.ArrayList;
import java.util.HashMap;

import me.sunzheng.mana.core.VideoFile;

/**
 * Created by Sun on 2018/1/9.
 */

public class LocalDataRepository implements DataRepository {
    MediaDescriptionCompat[] items;
    int currentMediaDescription = -1;
    int currentSource = -1;
    ArrayList<VideoFile> videoFiles;
    HashMap<String, VideoFile> labelMaps;

    public LocalDataRepository(MediaDescriptionCompat[] items) {
        this.items = items;
        videoFiles = new ArrayList<>();
        labelMaps = new HashMap<>();
    }

    @Override
    public int getCurrentPosition() {
        return currentMediaDescription;
    }

    @Override
    public void setCurrentPosition(int position) {
        currentMediaDescription = position;
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
    public void clearVideoFiles() {
        labelMaps.clear();
        videoFiles.clear();
    }

    @Override
    public void addVideoFile(VideoFile file) {
        videoFiles.add(file);
        labelMaps.put(file.getLabel(), file);
    }

    @Override
    public VideoFile getVideoFileItem(int position) {
        if (position < videoFiles.size())
            return videoFiles.get(position);
        else
            return videoFiles.get(0);
    }

    @Override
    public VideoFile getVideoFileItem(String label) {
        if (labelMaps.containsKey(label)) {
            return labelMaps.get(label);
        }
        return videoFiles.get(0);
    }

    @Override
    public int getCurrentSourcePosition() {
        if (!labelMaps.containsKey(currentSource))
            currentSource = 0;
        return currentSource;
    }

    @Override
    public void setCurrentSourcePosition(int position) {
        currentSource = position;
    }

    @Override
    public int getEpisodeCount() {
        return items.length;
    }

    @Override
    public String[] getSourceLabels() {
        return labelMaps.keySet().toArray(new String[labelMaps.size()]);
    }

    @Override
    public int getVideoFilesCount() {
        return videoFiles == null ? 0 : videoFiles.size();
    }
}

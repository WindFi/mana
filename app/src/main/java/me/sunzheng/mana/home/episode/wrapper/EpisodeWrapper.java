package me.sunzheng.mana.home.episode.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import me.sunzheng.mana.core.BangumiModel;
import me.sunzheng.mana.core.Episode;
import me.sunzheng.mana.core.VideoFile;

public class EpisodeWrapper extends Episode {

    @SerializedName("bangumi")
    @Expose
    private BangumiModel bangumi;
    @SerializedName("video_files")
    @Expose
    private List<VideoFile> videoFiles = null;


    public BangumiModel getBangumi() {
        return bangumi;
    }

    public void setBangumi(BangumiModel bangumi) {
        this.bangumi = bangumi;
    }

    public List<VideoFile> getVideoFiles() {
        return videoFiles;
    }

    public void setVideoFiles(List<VideoFile> videoFiles) {
        this.videoFiles = videoFiles;
    }
}

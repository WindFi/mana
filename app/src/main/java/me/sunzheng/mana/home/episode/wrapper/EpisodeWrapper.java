package me.sunzheng.mana.home.episode.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import me.sunzheng.mana.home.bangumi.wrapper.WatchProgress;

public class EpisodeWrapper {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("bangumi_id")
    @Expose
    private String bangumiId;
    @SerializedName("bgm_eps_id")
    @Expose
    private Long bgmEpsId;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("name_cn")
    @Expose
    private String nameCn;
    @SerializedName("thumbnail")
    @Expose
    private String thumbnail;
    @SerializedName("status")
    @Expose
    private Long status;
    @SerializedName("episode_no")
    @Expose
    private Long episodeNo;
    @SerializedName("create_time")
    @Expose
    private Long createTime;
    @SerializedName("update_time")
    @Expose
    private Long updateTime;
    @SerializedName("airdate")
    @Expose
    private String airdate;
    @SerializedName("delete_mark")
    @Expose
    private Long deleteMark;
    @SerializedName("duration")
    @Expose
    private String duration;
    @SerializedName("bangumi")
    @Expose
    private Bangumi bangumi;
    @SerializedName("video_files")
    @Expose
    private List<VideoFile> videoFiles = null;
    @SerializedName("watch_progress")
    @Expose
    private WatchProgress watch_process = null;
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBangumiId() {
        return bangumiId;
    }

    public void setBangumiId(String bangumiId) {
        this.bangumiId = bangumiId;
    }

    public Long getBgmEpsId() {
        return bgmEpsId;
    }

    public void setBgmEpsId(Long bgmEpsId) {
        this.bgmEpsId = bgmEpsId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameCn() {
        return nameCn;
    }

    public void setNameCn(String nameCn) {
        this.nameCn = nameCn;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public Long getEpisodeNo() {
        return episodeNo;
    }

    public void setEpisodeNo(Long episodeNo) {
        this.episodeNo = episodeNo;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public String getAirdate() {
        return airdate;
    }

    public void setAirdate(String airdate) {
        this.airdate = airdate;
    }

    public Long getDeleteMark() {
        return deleteMark;
    }

    public void setDeleteMark(Long deleteMark) {
        this.deleteMark = deleteMark;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public Bangumi getBangumi() {
        return bangumi;
    }

    public void setBangumi(Bangumi bangumi) {
        this.bangumi = bangumi;
    }

    public List<VideoFile> getVideoFiles() {
        return videoFiles;
    }

    public void setVideoFiles(List<VideoFile> videoFiles) {
        this.videoFiles = videoFiles;
    }

    public WatchProgress getWatch_process() {
        return watch_process;
    }

    public void setWatch_process(WatchProgress watch_process) {
        this.watch_process = watch_process;
    }
}


package me.sunzheng.mana.core;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class Episode {
    @SerializedName("status")
    @Expose
    private long status;
    @SerializedName("episode_no")
    @Expose
    private int episodeNo;
    @SerializedName("update_time")
    @Expose
    private float updateTime;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("bgm_eps_id")
    @Expose
    private long bgmEpsId;
    @SerializedName("bangumi_id")
    @Expose
    private UUID bangumiId;
    @SerializedName("airdate")
    @Expose
    private String airdate;
    @SerializedName("name_cn")
    @Expose
    private String nameCn;
    @SerializedName("thumbnail")
    @Expose
    private String thumbnail;
    @SerializedName("thumbnail_color")
    @Expose
    private String thumbnailColor;
    @SerializedName("delete_mark")
    @Expose
    private Object deleteMark;
    @SerializedName("create_time")
    @Expose
    private float createTime;
    @SerializedName("duration")
    @Expose
    private String duration;
    @SerializedName("id")
    @Expose
    private UUID id;
    @SerializedName("watch_progress")
    @Expose
    private WatchProgress watchProgress;
    @SerializedName("thumbnail_image")
    @Expose
    private CoverImage thumbnailImage;

    public CoverImage getThumbnailImage() {
        return thumbnailImage;
    }

    public void setThumbnailImage(CoverImage thumbnailImage) {
        this.thumbnailImage = thumbnailImage;
    }

    public WatchProgress getWatchProgress() {
        return watchProgress;
    }

    public void setWatchProgress(WatchProgress watchProgress) {
        this.watchProgress = watchProgress;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public int getEpisodeNo() {
        return episodeNo;
    }

    public void setEpisodeNo(int episodeNo) {
        this.episodeNo = episodeNo;
    }

    public Float getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Float updateTime) {
        this.updateTime = updateTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getBgmEpsId() {
        return bgmEpsId;
    }

    public void setBgmEpsId(Long bgmEpsId) {
        this.bgmEpsId = bgmEpsId;
    }

    public String getBangumiId() {
        return bangumiId.toString();
    }

    public void setBangumiId(String bangumiId) {
        this.bangumiId = UUID.fromString(bangumiId);
    }

    public String getAirdate() {
        return airdate;
    }

    public void setAirdate(String airdate) {
        this.airdate = airdate;
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

    public Object getDeleteMark() {
        return deleteMark;
    }

    public void setDeleteMark(Object deleteMark) {
        this.deleteMark = deleteMark;
    }

    public Float getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Float createTime) {
        this.createTime = createTime;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getId() {
        return id.toString();
    }

    public void setId(String id) {
        this.id = UUID.fromString(id);
    }

    public String getThumbnailColor() {
        return thumbnailColor;
    }

    public void setThumbnailColor(String thumbnailColor) {
        this.thumbnailColor = thumbnailColor;
    }
}

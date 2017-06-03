
package me.sunzheng.mana.home.bangumi.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Episode {
    @SerializedName("status")
    @Expose
    private Long status;
    @SerializedName("episode_no")
    @Expose
    private Long episodeNo;
    @SerializedName("update_time")
    @Expose
    private Float updateTime;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("bgm_eps_id")
    @Expose
    private Long bgmEpsId;
    @SerializedName("bangumi_id")
    @Expose
    private String bangumiId;
    @SerializedName("airdate")
    @Expose
    private String airdate;
    @SerializedName("name_cn")
    @Expose
    private String nameCn;
    @SerializedName("thumbnail")
    @Expose
    private String thumbnail;
    @SerializedName("delete_mark")
    @Expose
    private Object deleteMark;
    @SerializedName("create_time")
    @Expose
    private Float createTime;
    @SerializedName("duration")
    @Expose
    private String duration;
    @SerializedName("id")
    @Expose
    private String id;

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
        return bangumiId;
    }

    public void setBangumiId(String bangumiId) {
        this.bangumiId = bangumiId;
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
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}

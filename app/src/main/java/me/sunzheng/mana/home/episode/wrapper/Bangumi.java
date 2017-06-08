
package me.sunzheng.mana.home.episode.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Bangumi {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("bgm_id")
    @Expose
    private Long bgmId;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("name_cn")
    @Expose
    private String nameCn;
    @SerializedName("summary")
    @Expose
    private String summary;
    @SerializedName("image")
    @Expose
    private String image;
    @SerializedName("cover")
    @Expose
    private String cover;
    @SerializedName("create_time")
    @Expose
    private Long createTime;
    @SerializedName("update_time")
    @Expose
    private Long updateTime;
    @SerializedName("eps_no_offset")
    @Expose
    private Long epsNoOffset;
    @SerializedName("bangumi_moe")
    @Expose
    private String bangumiMoe;
    @SerializedName("libyk_so")
    @Expose
    private String libykSo;
    @SerializedName("dmhy")
    @Expose
    private String dmhy;
    @SerializedName("type")
    @Expose
    private Long type;
    @SerializedName("status")
    @Expose
    private Long status;
    @SerializedName("air_date")
    @Expose
    private String airDate;
    @SerializedName("air_weekday")
    @Expose
    private Long airWeekday;
    @SerializedName("delete_mark")
    @Expose
    private Long deleteMark;
    @SerializedName("acg_rip")
    @Expose
    private String acgRip;
    @SerializedName("rss")
    @Expose
    private String rss;
    @SerializedName("eps_regex")
    @Expose
    private String epsRegex;
    @SerializedName("eps")
    @Expose
    private Long eps;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getBgmId() {
        return bgmId;
    }

    public void setBgmId(Long bgmId) {
        this.bgmId = bgmId;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
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

    public Long getEpsNoOffset() {
        return epsNoOffset;
    }

    public void setEpsNoOffset(Long epsNoOffset) {
        this.epsNoOffset = epsNoOffset;
    }

    public String getBangumiMoe() {
        return bangumiMoe;
    }

    public void setBangumiMoe(String bangumiMoe) {
        this.bangumiMoe = bangumiMoe;
    }

    public String getLibykSo() {
        return libykSo;
    }

    public void setLibykSo(String libykSo) {
        this.libykSo = libykSo;
    }

    public String getDmhy() {
        return dmhy;
    }

    public void setDmhy(String dmhy) {
        this.dmhy = dmhy;
    }

    public Long getType() {
        return type;
    }

    public void setType(Long type) {
        this.type = type;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public String getAirDate() {
        return airDate;
    }

    public void setAirDate(String airDate) {
        this.airDate = airDate;
    }

    public Long getAirWeekday() {
        return airWeekday;
    }

    public void setAirWeekday(Long airWeekday) {
        this.airWeekday = airWeekday;
    }

    public Long getDeleteMark() {
        return deleteMark;
    }

    public void setDeleteMark(Long deleteMark) {
        this.deleteMark = deleteMark;
    }

    public String getAcgRip() {
        return acgRip;
    }

    public void setAcgRip(String acgRip) {
        this.acgRip = acgRip;
    }

    public String getRss() {
        return rss;
    }

    public void setRss(String rss) {
        this.rss = rss;
    }

    public String getEpsRegex() {
        return epsRegex;
    }

    public void setEpsRegex(String epsRegex) {
        this.epsRegex = epsRegex;
    }

    public Long getEps() {
        return eps;
    }

    public void setEps(Long eps) {
        this.eps = eps;
    }

}

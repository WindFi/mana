
package me.sunzheng.mana.home.search.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Datum {

    @SerializedName("image")
    @Expose
    private String image;
    @SerializedName("create_time")
    @Expose
    private Double createTime;
    @SerializedName("eps_no_offset")
    @Expose
    private Object epsNoOffset;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("bgm_id")
    @Expose
    private Long bgmId;
    @SerializedName("name_cn")
    @Expose
    private String nameCn;
    @SerializedName("bangumi_moe")
    @Expose
    private String bangumiMoe;
    @SerializedName("type")
    @Expose
    private Long type;
    @SerializedName("status")
    @Expose
    private Long status;
    @SerializedName("update_time")
    @Expose
    private Double updateTime;
    @SerializedName("air_date")
    @Expose
    private String airDate;
    @SerializedName("cover_color")
    @Expose
    private String coverColor;
    @SerializedName("delete_mark")
    @Expose
    private Object deleteMark;
    @SerializedName("acg_rip")
    @Expose
    private Object acgRip;
    @SerializedName("rss")
    @Expose
    private Object rss;
    @SerializedName("eps_regex")
    @Expose
    private Object epsRegex;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("cover")
    @Expose
    private String cover;
    @SerializedName("eps")
    @Expose
    private Long eps;
    @SerializedName("summary")
    @Expose
    private String summary;
    @SerializedName("favorite_status")
    @Expose
    private Long favoriteStatus;
    @SerializedName("air_weekday")
    @Expose
    private Long airWeekday;
    @SerializedName("libyk_so")
    @Expose
    private Object libykSo;
    @SerializedName("dmhy")
    @Expose
    private Object dmhy;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Double getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Double createTime) {
        this.createTime = createTime;
    }

    public Object getEpsNoOffset() {
        return epsNoOffset;
    }

    public void setEpsNoOffset(Object epsNoOffset) {
        this.epsNoOffset = epsNoOffset;
    }

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

    public String getNameCn() {
        return nameCn;
    }

    public void setNameCn(String nameCn) {
        this.nameCn = nameCn;
    }

    public String getBangumiMoe() {
        return bangumiMoe;
    }

    public void setBangumiMoe(String bangumiMoe) {
        this.bangumiMoe = bangumiMoe;
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

    public Double getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Double updateTime) {
        this.updateTime = updateTime;
    }

    public String getAirDate() {
        return airDate;
    }

    public void setAirDate(String airDate) {
        this.airDate = airDate;
    }

    public String getCoverColor() {
        return coverColor;
    }

    public void setCoverColor(String coverColor) {
        this.coverColor = coverColor;
    }

    public Object getDeleteMark() {
        return deleteMark;
    }

    public void setDeleteMark(Object deleteMark) {
        this.deleteMark = deleteMark;
    }

    public Object getAcgRip() {
        return acgRip;
    }

    public void setAcgRip(Object acgRip) {
        this.acgRip = acgRip;
    }

    public Object getRss() {
        return rss;
    }

    public void setRss(Object rss) {
        this.rss = rss;
    }

    public Object getEpsRegex() {
        return epsRegex;
    }

    public void setEpsRegex(Object epsRegex) {
        this.epsRegex = epsRegex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public Long getEps() {
        return eps;
    }

    public void setEps(Long eps) {
        this.eps = eps;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Long getFavoriteStatus() {
        return favoriteStatus;
    }

    public void setFavoriteStatus(Long favoriteStatus) {
        this.favoriteStatus = favoriteStatus;
    }

    public Long getAirWeekday() {
        return airWeekday;
    }

    public void setAirWeekday(Long airWeekday) {
        this.airWeekday = airWeekday;
    }

    public Object getLibykSo() {
        return libykSo;
    }

    public void setLibykSo(Object libykSo) {
        this.libykSo = libykSo;
    }

    public Object getDmhy() {
        return dmhy;
    }

    public void setDmhy(Object dmhy) {
        this.dmhy = dmhy;
    }

}

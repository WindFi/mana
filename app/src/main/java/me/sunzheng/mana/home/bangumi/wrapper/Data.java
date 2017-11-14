
package me.sunzheng.mana.home.bangumi.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.UUID;

public class Data {

    @SerializedName("image")
    @Expose
    private String image;
    @SerializedName("episodes")
    @Expose
    private List<Episode> episodes = null;
    @SerializedName("create_time")
    @Expose
    private Float createTime;
    @SerializedName("eps_no_offset")
    @Expose
    private Object epsNoOffset;
    @SerializedName("id")
    @Expose
    private UUID id;
    @SerializedName("bgm_id")
    @Expose
    private long bgmId;
    @SerializedName("name_cn")
    @Expose
    private String nameCn;
    @SerializedName("bangumi_moe")
    @Expose
    private String bangumiMoe;
    @SerializedName("type")
    @Expose
    private long type;
    @SerializedName("status")
    @Expose
    private long status;
    @SerializedName("update_time")
    @Expose
    private long updateTime;
    @SerializedName("air_date")
    @Expose
    private String airDate;
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
    private int eps;
    @SerializedName("summary")
    @Expose
    private String summary;
    @SerializedName("favorite_status")
    @Expose
    private long favoriteStatus;
    @SerializedName("air_weekday")
    @Expose
    private long airWeekday;
    @SerializedName("libyk_so")
    @Expose
    private Object libykSo;
    @SerializedName("dmhy")
    @Expose
    private Object dmhy;

    @SerializedName("cover_image")
    @Expose
    private ImageInfo cover_image;

    public ImageInfo getCover_image() {
        return cover_image;
    }

    public void setCover_image(ImageInfo cover_image) {
        this.cover_image = cover_image;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
    }

    public Float getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Float createTime) {
        this.createTime = createTime;
    }

    public Object getEpsNoOffset() {
        return epsNoOffset;
    }

    public void setEpsNoOffset(Object epsNoOffset) {
        this.epsNoOffset = epsNoOffset;
    }

    public String getId() {
        return id.toString();
    }

    public void setId(String id) {
        this.id = UUID.fromString(id);
    }

    public long getBgmId() {
        return bgmId;
    }

    public void setBgmId(long bgmId) {
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

    public long getType() {
        return type;
    }

    public void setType(long type) {
        this.type = type;
    }

    public long getStatus() {
        return status;
    }

    public void setStatus(long status) {
        this.status = status;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public String getAirDate() {
        return airDate;
    }

    public void setAirDate(String airDate) {
        this.airDate = airDate;
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

    public int getEps() {
        return eps;
    }

    public void setEps(int eps) {
        this.eps = eps;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public long getFavoriteStatus() {
        return favoriteStatus;
    }

    public void setFavoriteStatus(long favoriteStatus) {
        this.favoriteStatus = favoriteStatus;
    }

    public long getAirWeekday() {
        return airWeekday;
    }

    public void setAirWeekday(long airWeekday) {
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

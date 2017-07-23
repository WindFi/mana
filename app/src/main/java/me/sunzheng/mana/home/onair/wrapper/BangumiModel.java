
package me.sunzheng.mana.home.onair.wrapper;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class BangumiModel implements Parcelable {

    public static final Creator<BangumiModel> CREATOR = new Creator<BangumiModel>() {
        @Override
        public BangumiModel createFromParcel(Parcel in) {
            return new BangumiModel(in);
        }

        @Override
        public BangumiModel[] newArray(int size) {
            return new BangumiModel[size];
        }
    };
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("bgm_id")
    @Expose
    private Integer bgmId;
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
    @SerializedName("cover_color")
    @Expose
    private String cover_color;
    @SerializedName("create_time")
    @Expose
    private long createTime;
    @SerializedName("update_time")
    @Expose
    private long updateTime;
    @SerializedName("eps_no_offset")
    @Expose
    private long epsNoOffset;
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
    private Integer type;
    @SerializedName("status")
    @Expose
    private Integer status;
    @SerializedName("air_date")
    @Expose
    private String airDate;
    @SerializedName("air_weekday")
    @Expose
    private long airWeekday;
    @SerializedName("delete_mark")
    @Expose
    private long deleteMark;
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
    private Integer eps;
    @SerializedName("favorite_status")
    @Expose
    private Integer favoriteStatus;

    protected BangumiModel(Parcel in) {
        id = in.readString();
        name = in.readString();
        nameCn = in.readString();
        summary = in.readString();
        image = in.readString();
        cover = in.readString();
        cover_color = in.readString();
        createTime = in.readLong();
        updateTime = in.readLong();
        epsNoOffset = in.readLong();
        bangumiMoe = in.readString();
        libykSo = in.readString();
        dmhy = in.readString();
        airDate = in.readString();
        airWeekday = in.readLong();
        deleteMark = in.readLong();
        acgRip = in.readString();
        rss = in.readString();
        epsRegex = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(nameCn);
        dest.writeString(summary);
        dest.writeString(image);
        dest.writeString(cover);
        dest.writeString(cover_color);
        dest.writeLong(createTime);
        dest.writeLong(updateTime);
        dest.writeLong(epsNoOffset);
        dest.writeString(bangumiMoe);
        dest.writeString(libykSo);
        dest.writeString(dmhy);
        dest.writeString(airDate);
        dest.writeLong(airWeekday);
        dest.writeLong(deleteMark);
        dest.writeString(acgRip);
        dest.writeString(rss);
        dest.writeString(epsRegex);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getBgmId() {
        return bgmId;
    }

    public void setBgmId(Integer bgmId) {
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

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public long getEpsNoOffset() {
        return epsNoOffset;
    }

    public void setEpsNoOffset(long epsNoOffset) {
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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }


    public String getAirDate() {
        return airDate;
    }

    public void setAirDate(String airDate) {
        this.airDate = airDate;
    }

    public long getAirWeekday() {
        return airWeekday;
    }

    public void setAirWeekday(long airWeekday) {
        this.airWeekday = airWeekday;
    }

    public long getDeleteMark() {
        return deleteMark;
    }

    public void setDeleteMark(long deleteMark) {
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

    public Integer getEps() {
        return eps;
    }

    public void setEps(Integer eps) {
        this.eps = eps;
    }

    public Integer getFavoriteStatus() {
        return favoriteStatus;
    }

    public void setFavoriteStatus(Integer favoriteStatus) {
        this.favoriteStatus = favoriteStatus;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getCover_color() {
        return cover_color;
    }

    public void setCover_color(String cover_color) {
        this.cover_color = cover_color;
    }
}

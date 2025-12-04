package me.sunzheng.mana.core;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.UUID;

@DatabaseTable(tableName = "favorites")
public class BangumiModel implements Parcelable {

    public static final Creator<BangumiModel> CREATOR = new Creator<BangumiModel>() {
        @NonNull
        @Override
        public BangumiModel createFromParcel(@NonNull Parcel in) {
            return new BangumiModel(in);
        }

        @Override
        public BangumiModel[] newArray(int size) {
            return new BangumiModel[size];
        }
    };
    @DatabaseField(generatedId = true)
    protected long _id;
    @DatabaseField(columnName = "id", dataType = DataType.UUID)
    @SerializedName("id")
    @Expose
    protected UUID id;
    @DatabaseField(columnName = "bgm_id", dataType = DataType.INTEGER)
    @SerializedName("bgm_id")
    @Expose
    protected int bgmId;
    @DatabaseField(columnName = "name")
    @SerializedName("name")
    @Expose
    protected String name;
    @DatabaseField(columnName = "name_cn")
    @SerializedName("name_cn")
    @Expose
    protected String nameCn;
    @DatabaseField(columnName = "summary")
    @SerializedName("summary")
    @Expose
    protected String summary;
    @DatabaseField(columnName = "image")
    @SerializedName("image")
    @Expose
    protected String image;
    @DatabaseField(columnName = "cover")
    @SerializedName("cover")
    @Expose
    protected String cover;
    @DatabaseField(columnName = "cover_color")
    @SerializedName("cover_color")
    @Expose
    protected String cover_color;
    @DatabaseField(columnName = "create_time", dataType = DataType.LONG)
    @SerializedName("create_time")
    @Expose
    protected long createTime;
    @DatabaseField(columnName = "update_time", dataType = DataType.LONG)
    @SerializedName("update_time")
    @Expose
    protected long updateTime;
    @DatabaseField(columnName = "eps_no_offset", dataType = DataType.LONG)
    @SerializedName("eps_no_offset")
    @Expose
    protected long epsNoOffset;
    @DatabaseField(columnName = "bangumi_moe")
    @SerializedName("bangumi_moe")
    @Expose
    protected String bangumiMoe;
    @DatabaseField(columnName = "libyk_so")
    @SerializedName("libyk_so")
    @Expose
    protected String libykSo;
    @DatabaseField(columnName = "dmhy")
    @SerializedName("dmhy")
    @Expose
    protected String dmhy;
    @DatabaseField(columnName = "type", dataType = DataType.INTEGER)
    @SerializedName("type")
    @Expose
    protected int type;
    @DatabaseField(columnName = "status", dataType = DataType.INTEGER)
    @SerializedName("status")
    @Expose
    protected int status;
    @DatabaseField(columnName = "air_date")
    @SerializedName("air_date")
    @Expose
    protected String airDate;
    @DatabaseField(columnName = "air_weekday", dataType = DataType.LONG)
    @SerializedName("air_weekday")
    @Expose
    protected long airWeekday;
    @DatabaseField(columnName = "delete_mark", dataType = DataType.LONG)
    @SerializedName("delete_mark")
    @Expose
    protected long deleteMark;
    @DatabaseField(columnName = "acg_rip")
    @SerializedName("acg_rip")
    @Expose
    protected String acgRip;
    @DatabaseField(columnName = "rss")
    @SerializedName("rss")
    @Expose
    protected String rss;
    @DatabaseField(columnName = "eps_regex")
    @SerializedName("eps_regex")
    @Expose
    protected String epsRegex;
    @DatabaseField(columnName = "eps", dataType = DataType.INTEGER)
    @SerializedName("eps")
    @Expose
    protected int eps;
    @DatabaseField(columnName = "favorite_status", dataType = DataType.INTEGER)
    @SerializedName("favorite_status")
    @Expose
    protected int favoriteStatus;

    @DatabaseField(columnName = "unwatched_count", dataType = DataType.INTEGER)
    @SerializedName("unwatched_count")
    @Expose
    protected int unwatched_count;

    @SerializedName("cover_image")
    @Expose
    protected CoverImage coverImage;

    @SerializedName("favorite_update_time")
    @Expose
    protected long favoriteUpdateTime;
    public BangumiModel() {
    }

    public BangumiModel(@NonNull Parcel in) {
        _id = in.readLong();
        id = UUID.fromString(in.readString());
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
        unwatched_count = in.readInt();
        coverImage = in.readParcelable(ClassLoader.getSystemClassLoader());
        favoriteUpdateTime = in.readLong();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(_id);
        dest.writeString(id.toString());
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
        dest.writeInt(unwatched_count);
        dest.writeParcelable(coverImage, 0);
        dest.writeLong(favoriteUpdateTime);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public String getId() {
        return id.toString();
    }

    public void setId(String id) {
        this.id = UUID.fromString(id);
    }

    public int getBgmId() {
        return bgmId;
    }

    public void setBgmId(int bgmId) {
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

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
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

    public int getEps() {
        return eps;
    }

    public void setEps(int eps) {
        this.eps = eps;
    }

    public int getFavoriteStatus() {
        return favoriteStatus;
    }

    public void setFavoriteStatus(int favoriteStatus) {
        this.favoriteStatus = favoriteStatus;
    }

    public int getUnwatched_count() {
        return unwatched_count;
    }

    public void setUnwatched_count(int unwatched_count) {
        this.unwatched_count = unwatched_count;
    }

    @NonNull
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

    public long get_id() {
        return _id;
    }

    public void set_id(long _id) {
        this._id = _id;
    }

    public CoverImage getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(CoverImage coverImage) {
        this.coverImage = coverImage;
    }

    public long getFavoriteUpdateTime(){
        return favoriteUpdateTime;
    }

    public void setFavoriteUpdateTime(long favoriteUpdateTime){
        this.favoriteUpdateTime = favoriteUpdateTime;
    }
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BangumiModel that = (BangumiModel) o;

        return id.equals(that.id);
    }
}
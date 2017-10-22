
package me.sunzheng.mana.home.bangumi;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WatchProgress implements Parcelable {

    public static final Creator<WatchProgress> CREATOR = new Creator<WatchProgress>() {
        @Override
        public WatchProgress createFromParcel(Parcel in) {
            return new WatchProgress(in);
        }

        @Override
        public WatchProgress[] newArray(int size) {
            return new WatchProgress[size];
        }
    };
    @SerializedName("user_id")
    @Expose
    private String userId;
    @SerializedName("last_watch_position")
    @Expose
    private double lastWatchPosition;
    @SerializedName("bangumi_id")
    @Expose
    private String bangumiId;
    @SerializedName("watch_status")
    @Expose
    private long watchStatus;
    @SerializedName("episode_id")
    @Expose
    private String episodeId;
    @SerializedName("percentage")
    @Expose
    private double percentage;
    @SerializedName("last_watch_time")
    @Expose
    private double lastWatchTime;
    @SerializedName("id")
    @Expose
    private String id;

    protected WatchProgress(Parcel in) {
        userId = in.readString();
        lastWatchPosition = in.readDouble();
        bangumiId = in.readString();
        watchStatus = in.readLong();
        episodeId = in.readString();
        percentage = in.readDouble();
        lastWatchTime = in.readDouble();
        id = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeDouble(lastWatchPosition);
        dest.writeString(bangumiId);
        dest.writeLong(watchStatus);
        dest.writeString(episodeId);
        dest.writeDouble(percentage);
        dest.writeDouble(lastWatchTime);
        dest.writeString(id);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getLastWatchPosition() {
        return lastWatchPosition;
    }

    public void setLastWatchPosition(double lastWatchPosition) {
        this.lastWatchPosition = lastWatchPosition;
    }

    public String getBangumiId() {
        return bangumiId;
    }

    public void setBangumiId(String bangumiId) {
        this.bangumiId = bangumiId;
    }

    public long getWatchStatus() {
        return watchStatus;
    }

    public void setWatchStatus(long watchStatus) {
        this.watchStatus = watchStatus;
    }

    public String getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = episodeId;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    public double getLastWatchTime() {
        return lastWatchTime;
    }

    public void setLastWatchTime(double lastWatchTime) {
        this.lastWatchTime = lastWatchTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}

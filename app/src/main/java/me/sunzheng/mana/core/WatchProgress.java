
package me.sunzheng.mana.core;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class WatchProgress implements Parcelable {

    public static final Creator<WatchProgress> CREATOR = new Creator<WatchProgress>() {
        @NonNull
        @Override
        public WatchProgress createFromParcel(@NonNull Parcel in) {
            return new WatchProgress(in);
        }

        @Override
        public WatchProgress[] newArray(int size) {
            return new WatchProgress[size];
        }
    };
    @SerializedName("user_id")
    @Expose
    private UUID userId;
    @SerializedName("last_watch_position")
    @Expose
    private double lastWatchPosition;
    @SerializedName("bangumi_id")
    @Expose
    private UUID bangumiId;
    @SerializedName("watch_status")
    @Expose
    private long watchStatus;
    @SerializedName("episode_id")
    @Expose
    private UUID episodeId;
    @SerializedName("percentage")
    @Expose
    private double percentage;
    @SerializedName("last_watch_time")
    @Expose
    private double lastWatchTime;
    @SerializedName("id")
    @Expose
    private UUID id;

    protected WatchProgress(@NonNull Parcel in) {
        userId = UUID.fromString(in.readString());
        lastWatchPosition = in.readDouble();
        bangumiId = UUID.fromString(in.readString());
        watchStatus = in.readLong();
        episodeId = UUID.fromString(in.readString());
        percentage = in.readDouble();
        lastWatchTime = in.readDouble();
        id = UUID.fromString(in.readString());
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(userId.toString());
        dest.writeDouble(lastWatchPosition);
        dest.writeString(bangumiId.toString());
        dest.writeLong(watchStatus);
        dest.writeString(episodeId.toString());
        dest.writeDouble(percentage);
        dest.writeDouble(lastWatchTime);
        dest.writeString(id.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @NonNull
    public String getUserId() {
        return userId.toString();
    }

    public void setUserId(String userId) {
        this.userId = UUID.fromString(userId);
    }

    public double getLastWatchPosition() {
        return lastWatchPosition;
    }

    public void setLastWatchPosition(double lastWatchPosition) {
        this.lastWatchPosition = lastWatchPosition;
    }

    @NonNull
    public String getBangumiId() {
        return bangumiId.toString();
    }

    public void setBangumiId(String bangumiId) {
        this.bangumiId = UUID.fromString(bangumiId);
    }

    public long getWatchStatus() {
        return watchStatus;
    }

    public void setWatchStatus(long watchStatus) {
        this.watchStatus = watchStatus;
    }

    @NonNull
    public String getEpisodeId() {
        return episodeId.toString();
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = UUID.fromString(episodeId);
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

    @NonNull
    public String getId() {
        return id.toString();
    }

    public void setId(String id) {
        this.id = UUID.fromString(id);
    }

}

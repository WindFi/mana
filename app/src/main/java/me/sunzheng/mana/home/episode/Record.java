package me.sunzheng.mana.home.episode;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.UUID;

/**
 * Created by Sun on 2017/7/28.
 */

public class Record implements Parcelable {

    public static final Creator<Record> CREATOR = new Creator<Record>() {
        @Override
        public Record createFromParcel(Parcel in) {
            return new Record(in);
        }

        @Override
        public Record[] newArray(int size) {
            return new Record[size];
        }
    };
    @SerializedName("bangumi_id")
    @Expose
    private UUID bangumiId;
    @SerializedName("episode_id")
    @Expose
    private UUID episodeId;
    @SerializedName("last_watch_position")
    @Expose
    private float lastWatchPosition;
    @SerializedName("last_watch_time")
    @Expose
    private long lastWatchTime;
    @SerializedName("percentage")
    @Expose
    private float percentage;
    @SerializedName("is_finished")
    @Expose
    private boolean isFinished;

    public Record() {
    }

    protected Record(Parcel in) {
        bangumiId = UUID.fromString(in.readString());
        episodeId = UUID.fromString(in.readString());
        lastWatchPosition = in.readFloat();
        lastWatchTime = in.readLong();
        percentage = in.readFloat();
        isFinished = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(bangumiId.toString());
        dest.writeString(episodeId.toString());
        dest.writeFloat(lastWatchPosition);
        dest.writeLong(lastWatchTime);
        dest.writeFloat(percentage);
        dest.writeByte((byte) (isFinished ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public String getBangumiId() {
        return bangumiId.toString();
    }

    public void setBangumiId(String bangumiId) {
        this.bangumiId = UUID.fromString(bangumiId);
    }

    public String getEpisodeId() {
        return episodeId.toString();
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = UUID.fromString(episodeId);
    }

    public float getLastWatchPosition() {
        return lastWatchPosition;
    }

    public void setLastWatchPosition(float lastWatchPosition) {
        this.lastWatchPosition = lastWatchPosition;
    }

    public long getLastWatchTime() {
        return lastWatchTime;
    }

    public void setLastWatchTime(long lastWatchTime) {
        this.lastWatchTime = lastWatchTime;
    }

    public float getPercentage() {
        return percentage;
    }

    public void setPercentage(float percentage) {
        this.percentage = percentage;
    }

    public boolean getIsFinished() {
        return isFinished;
    }

    public void setIsFinished(boolean isFinished) {
        this.isFinished = isFinished;
    }

}

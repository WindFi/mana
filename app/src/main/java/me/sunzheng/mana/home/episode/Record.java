package me.sunzheng.mana.home.episode;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by Sun on 2017/7/28.
 */

public class Record implements Parcelable {

    public final static Parcelable.Creator<Record> CREATOR = new Creator<Record>() {


        @SuppressWarnings({
                "unchecked"
        })
        public Record createFromParcel(Parcel in) {
            Record instance = new Record();
            instance.bangumiId = ((String) in.readValue((String.class.getClassLoader())));
            instance.episodeId = ((String) in.readValue((String.class.getClassLoader())));
            instance.lastWatchPosition = ((Float) in.readValue((Float.class.getClassLoader())));
            instance.lastWatchTime = ((Integer) in.readValue((Integer.class.getClassLoader())));
            instance.percentage = ((Integer) in.readValue((Integer.class.getClassLoader())));
            instance.isFinished = ((Boolean) in.readValue((Boolean.class.getClassLoader())));
            return instance;
        }

        public Record[] newArray(int size) {
            return (new Record[size]);
        }

    };
    @SerializedName("bangumi_id")
    @Expose
    private String bangumiId;
    @SerializedName("episode_id")
    @Expose
    private String episodeId;
    @SerializedName("last_watch_position")
    @Expose
    private Float lastWatchPosition;
    @SerializedName("last_watch_time")
    @Expose
    private Integer lastWatchTime;
    @SerializedName("percentage")
    @Expose
    private Integer percentage;
    @SerializedName("is_finished")
    @Expose
    private Boolean isFinished;

    public String getBangumiId() {
        return bangumiId;
    }

    public void setBangumiId(String bangumiId) {
        this.bangumiId = bangumiId;
    }

    public String getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = episodeId;
    }

    public Float getLastWatchPosition() {
        return lastWatchPosition;
    }

    public void setLastWatchPosition(Float lastWatchPosition) {
        this.lastWatchPosition = lastWatchPosition;
    }

    public Integer getLastWatchTime() {
        return lastWatchTime;
    }

    public void setLastWatchTime(Integer lastWatchTime) {
        this.lastWatchTime = lastWatchTime;
    }

    public Integer getPercentage() {
        return percentage;
    }

    public void setPercentage(Integer percentage) {
        this.percentage = percentage;
    }

    public Boolean getIsFinished() {
        return isFinished;
    }

    public void setIsFinished(Boolean isFinished) {
        this.isFinished = isFinished;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(bangumiId);
        dest.writeValue(episodeId);
        dest.writeValue(lastWatchPosition);
        dest.writeValue(lastWatchTime);
        dest.writeValue(percentage);
        dest.writeValue(isFinished);
    }

    public int describeContents() {
        return 0;
    }

}

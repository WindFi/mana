package me.sunzheng.mana.home.bangumi.wrapper.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.UUID;

/**
 * Created by Sun on 2017/11/15.
 */
@DatabaseTable(tableName = "watch_progress")
public class WatchRecord {
    @DatabaseField(columnName = "bangumi_id", dataType = DataType.UUID)
    @SerializedName("bangumi_id")
    @Expose
    private UUID bangumiId;
    @DatabaseField(columnName = "episode_id", dataType = DataType.UUID)
    @SerializedName("episode_id")
    @Expose
    private UUID episodeId;
    @DatabaseField(columnName = "last_watch_position", dataType = DataType.FLOAT)
    @SerializedName("last_watch_position")
    @Expose
    private float lastWatchPosition;
    @DatabaseField(columnName = "last_watch_time", dataType = DataType.LONG)
    @SerializedName("last_watch_time")
    @Expose
    private long lastWatchTime;
    @DatabaseField(columnName = "percentage", dataType = DataType.FLOAT)
    @SerializedName("percentage")
    @Expose
    private float percentage;
    @DatabaseField(columnName = "is_finished", dataType = DataType.BOOLEAN)
    @SerializedName("is_finished")
    @Expose
    private boolean isFinished;
    @DatabaseField(columnName = "is_commited", dataType = DataType.BOOLEAN)
    private boolean isCommited;
    @DatabaseField(generatedId = true)
    private long _id;

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

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public boolean isCommited() {
        return isCommited;
    }

    public void setCommited(boolean commited) {
        isCommited = commited;
    }

    public long get_id() {
        return _id;
    }

    public void set_id(long _id) {
        this._id = _id;
    }
}

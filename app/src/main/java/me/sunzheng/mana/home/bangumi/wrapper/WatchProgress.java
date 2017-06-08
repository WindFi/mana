
package me.sunzheng.mana.home.bangumi.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WatchProgress {

    @SerializedName("user_id")
    @Expose
    private String userId;
    @SerializedName("last_watch_position")
    @Expose
    private Double lastWatchPosition;
    @SerializedName("bangumi_id")
    @Expose
    private String bangumiId;
    @SerializedName("watch_status")
    @Expose
    private Long watchStatus;
    @SerializedName("episode_id")
    @Expose
    private String episodeId;
    @SerializedName("percentage")
    @Expose
    private Double percentage;
    @SerializedName("last_watch_time")
    @Expose
    private Double lastWatchTime;
    @SerializedName("id")
    @Expose
    private String id;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getLastWatchPosition() {
        return lastWatchPosition;
    }

    public void setLastWatchPosition(Double lastWatchPosition) {
        this.lastWatchPosition = lastWatchPosition;
    }

    public String getBangumiId() {
        return bangumiId;
    }

    public void setBangumiId(String bangumiId) {
        this.bangumiId = bangumiId;
    }

    public Long getWatchStatus() {
        return watchStatus;
    }

    public void setWatchStatus(Long watchStatus) {
        this.watchStatus = watchStatus;
    }

    public String getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = episodeId;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public Double getLastWatchTime() {
        return lastWatchTime;
    }

    public void setLastWatchTime(Double lastWatchTime) {
        this.lastWatchTime = lastWatchTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}

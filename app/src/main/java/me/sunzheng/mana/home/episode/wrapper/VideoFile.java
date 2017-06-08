
package me.sunzheng.mana.home.episode.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class VideoFile {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("status")
    @Expose
    private Long status;
    @SerializedName("torrent_id")
    @Expose
    private String torrentId;
    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("file_path")
    @Expose
    private String filePath;
    @SerializedName("file_name")
    @Expose
    private String fileName;
    @SerializedName("resolution_w")
    @Expose
    private Long resolutionW;
    @SerializedName("download_url")
    @Expose
    private String downloadUrl;
    @SerializedName("episode_id")
    @Expose
    private String episodeId;
    @SerializedName("resolution_h")
    @Expose
    private Long resolutionH;
    @SerializedName("bangumi_id")
    @Expose
    private String bangumiId;
    @SerializedName("duration")
    @Expose
    private Long duration;
    @SerializedName("label")
    @Expose
    private String label;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public String getTorrentId() {
        return torrentId;
    }

    public void setTorrentId(String torrentId) {
        this.torrentId = torrentId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getResolutionW() {
        return resolutionW;
    }

    public void setResolutionW(Long resolutionW) {
        this.resolutionW = resolutionW;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(String episodeId) {
        this.episodeId = episodeId;
    }

    public Long getResolutionH() {
        return resolutionH;
    }

    public void setResolutionH(Long resolutionH) {
        this.resolutionH = resolutionH;
    }

    public String getBangumiId() {
        return bangumiId;
    }

    public void setBangumiId(String bangumiId) {
        this.bangumiId = bangumiId;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

}
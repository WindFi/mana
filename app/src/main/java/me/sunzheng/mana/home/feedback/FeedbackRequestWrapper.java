package me.sunzheng.mana.home.feedback;

import com.google.gson.annotations.Expose;

import java.util.UUID;

/**
 * Created by Sun on 2018/2/23.
 */

public class FeedbackRequestWrapper {
    @Expose
    private String episode_id;
    @Expose
    private String video_file_id;
    @Expose
    private String message;

    public String getEpisode_id() {
        return episode_id;
    }

    public void setEpisode_id(UUID episode_id) {
        this.episode_id = episode_id.toString();
    }

    public void setEpisode_id(String episode_id) {
        this.episode_id = episode_id;
    }

    public String getVideo_file_id() {
        return video_file_id;
    }

    public void setVideo_file_id(UUID video_file_id) {
        this.video_file_id = video_file_id.toString();
    }

    public void setVideo_file_id(String video_file_id) {
        this.video_file_id = video_file_id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

package me.sunzheng.mana.home.feedback;

import com.google.gson.annotations.Expose;

/**
 * Created by Sun on 2018/2/23.
 */

public class FeedbackResponseWrapper {
    @Expose
    private String message;
    @Expose
    private int status;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}

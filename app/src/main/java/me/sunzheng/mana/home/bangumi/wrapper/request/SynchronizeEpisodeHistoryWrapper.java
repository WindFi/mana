package me.sunzheng.mana.home.bangumi.wrapper.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by Sun on 2017/11/15.
 */

public class SynchronizeEpisodeHistoryWrapper {
    @SerializedName("records")
    @Expose
    private List<WatchRecord> item;

    public List<WatchRecord> getItem() {
        return item;
    }

    public void setItem(List<WatchRecord> item) {
        this.item = item;
    }
}

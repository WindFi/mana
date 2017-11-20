package me.sunzheng.mana.home.bangumi.wrapper.request;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import me.sunzheng.mana.home.episode.Record;

/**
 * Created by Sun on 2017/11/15.
 */

public class SynchronizeEpisodeHistoryWrapper {
    @SerializedName("records")
    @Expose
    private List<Record> item;

    public List<Record> getItem() {
        return item;
    }

    public void setItem(List<Record> item) {
        this.item = item;
    }
}

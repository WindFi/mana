
package me.sunzheng.mana.home.search;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import me.sunzheng.mana.home.onair.wrapper.BangumiModel;

public class SearchResultWrapper {

    @SerializedName("total")
    @Expose
    private int total;
    @SerializedName("data")
    @Expose
    private List<BangumiModel> data = null;

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<BangumiModel> getData() {
        return data;
    }

    public void setData(List<BangumiModel> data) {
        this.data = data;
    }

}


package me.sunzheng.mana.home.search;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import me.sunzheng.mana.home.search.wrapper.Datum;

public class SearchResultWrapper {

    @SerializedName("total")
    @Expose
    private Long total;
    @SerializedName("data")
    @Expose
    private List<Datum> data = null;

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<Datum> getData() {
        return data;
    }

    public void setData(List<Datum> data) {
        this.data = data;
    }

}

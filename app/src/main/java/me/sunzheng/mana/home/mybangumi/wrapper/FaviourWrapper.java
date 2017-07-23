
package me.sunzheng.mana.home.mybangumi.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import me.sunzheng.mana.home.onair.wrapper.BangumiModel;

public class FaviourWrapper {

    @SerializedName("status")
    @Expose
    private Long status;
    @SerializedName("data")
    @Expose
    private List<BangumiModel> data = null;

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

    public List<BangumiModel> getData() {
        return data;
    }

    public void setData(List<BangumiModel> data) {
        this.data = data;
    }

}

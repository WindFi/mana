
package me.sunzheng.mana.home.mybangumi.wrapper;

import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import me.sunzheng.mana.core.BangumiModel;

public class FavoriteWrapper {

    @SerializedName("status")
    @Expose
    private long status;
    @Nullable
    @SerializedName("data")
    @Expose
    private List<BangumiModel> data = null;

    public long getStatus() {
        return status;
    }

    public void setStatus(long status) {
        this.status = status;
    }

    public List<BangumiModel> getData() {
        return data;
    }

    public void setData(List<BangumiModel> data) {
        this.data = data;
    }

}

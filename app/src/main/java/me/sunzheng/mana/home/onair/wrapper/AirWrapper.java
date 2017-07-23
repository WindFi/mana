
package me.sunzheng.mana.home.onair.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public class AirWrapper {

    @SerializedName("data")
    @Expose
    private List<BangumiModel> data = null;

    public List<BangumiModel> getData() {
        return data;
    }

    public void setData(List<BangumiModel> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}

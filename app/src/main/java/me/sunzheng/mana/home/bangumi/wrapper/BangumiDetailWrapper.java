
package me.sunzheng.mana.home.bangumi.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class BangumiDetailWrapper {

    @SerializedName("data")
    @Expose
    private BangumiDetails bangumiDetails;

    public BangumiDetails getBangumiDetails() {
        return bangumiDetails;
    }

    public void setBangumiDetails(BangumiDetails bangumiDetails) {
        this.bangumiDetails = bangumiDetails;
    }

}


package me.sunzheng.mana.home.bangumi.wrapper;

import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import me.sunzheng.mana.core.BangumiModel;
import me.sunzheng.mana.core.Episode;

public class BangumiDetails extends BangumiModel {

    @Nullable
    @SerializedName("episodes")
    @Expose
    private List<Episode> episodes = null;

    public List<Episode> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(List<Episode> episodes) {
        this.episodes = episodes;
    }
}

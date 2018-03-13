package me.sunzheng.mana.core;

import com.google.gson.annotations.Expose;

import java.util.UUID;

/**
 * Created by Sun on 2018/3/8.
 */

public class AnnounceModel {
    @Expose
    private UUID id;
    @Expose
    private String content;
    //    position=1 is not null,if not will be null
    @Expose
    private String image_url;
    //    1 or 2
    @Expose
    private int position;
    //    for sort
    @Expose
    private int sort_order;
    @Expose
    private long start_time;
    @Expose
    private long end_time;
    @Expose
    private BangumiModel bangumi;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getSort_order() {
        return sort_order;
    }

    public void setSort_order(int sort_order) {
        this.sort_order = sort_order;
    }

    public long getStart_time() {
        return start_time;
    }

    public void setStart_time(long start_time) {
        this.start_time = start_time;
    }

    public long getEnd_time() {
        return end_time;
    }

    public void setEnd_time(long end_time) {
        this.end_time = end_time;
    }

    public BangumiModel getBangumi() {
        return bangumi;
    }

    public void setBangumi(BangumiModel bangumi) {
        this.bangumi = bangumi;
    }
}

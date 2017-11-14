package me.sunzheng.mana.home.bangumi.wrapper;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * A common po for image
 * Created by Sun on 2017/11/14.
 */

public class ImageInfo {
    @SerializedName("url")
    @Expose
    public String url;
    @SerializedName("dominant_color")
    @Expose
    public String dominantColor;
    @SerializedName("width")
    @Expose
    public int width;
    @SerializedName("height")
    @Expose
    public int height;


}

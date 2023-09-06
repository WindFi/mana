package me.sunzheng.mana.core;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * A common po for image
 * Created by Sun on 2017/11/14.
 */

public class CoverImage implements Parcelable {
    public static final Creator<CoverImage> CREATOR = new Creator<CoverImage>() {
        @NonNull
        @Override
        public CoverImage createFromParcel(@NonNull Parcel in) {
            return new CoverImage(in);
        }

        @Override
        public CoverImage[] newArray(int size) {
            return new CoverImage[size];
        }
    };
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

    protected CoverImage(@NonNull Parcel in) {
        url = in.readString();
        dominantColor = in.readString();
        width = in.readInt();
        height = in.readInt();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(dominantColor);
        dest.writeInt(width);
        dest.writeInt(height);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}

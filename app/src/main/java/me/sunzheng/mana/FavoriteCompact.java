package me.sunzheng.mana;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

/**
 * Created by Sun on 2017/7/6.
 */

public class FavoriteCompact {
    static int[] colors = new int[]{android.R.attr.buttonStyle, R.color.favorite_wish, R.color.favorite_watched, R.color.favorite_watching, R.color.favorite_pause, R.color.favorite_abanoned};

    public static void setFavorite(long status, TextView v) {
        String statusString = v.getResources().getStringArray(R.array.favorite_status_values)[(int) status];
        DrawableCompat drawableCompat = null;
        if (Build.VERSION.SDK_INT >= 21)
            drawableCompat = new LolipopDrawableCompatImpl(v.getContext());
        else
            drawableCompat = new BaseDrawableCompatImpl(v.getContext());
//        v.setBackground(drawableCompat.getDrawable(status,v));
//        switch ((int) status) {
//            case 1:
//                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.favorite_wish));
//                break;
//            case 2:
//                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.favorite_watched));
//                break;
//            case 3:
//                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.favorite_watching));
//                break;
//            case 4:
//                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.favorite_pause));
//                break;
//            case 5:
//                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.favorite_abanoned));
//                break;
//            default:
//                break;
//        }
        v.setText(statusString);
    }

    static abstract class DrawableCompat {
        private Context mContext;

        public DrawableCompat(Context mContext) {
            this.mContext = mContext;
        }

        public Context getContext() {
            return mContext;
        }

        abstract Drawable getDrawable(long status, TextView v);
    }

    static class BaseDrawableCompatImpl extends DrawableCompat {
        public BaseDrawableCompatImpl(Context mContext) {
            super(mContext);
        }

        @Override
        Drawable getDrawable(long status, TextView v) {
            return null;
        }
    }

    @TargetApi(21)
    static class LolipopDrawableCompatImpl extends DrawableCompat {
        public LolipopDrawableCompatImpl(Context mContext) {
            super(mContext);
        }

        @Override
        Drawable getDrawable(long status, TextView v) {
            int[][] cstatus = new int[][]{new int[]{android.R.attr.state_pressed}};
            return new RippleDrawable(new ColorStateList(cstatus, colors),
                    new ColorDrawable(ContextCompat.getColor(v.getContext(), colors[(int) status % colors.length])),
                    v.getBackground());
        }
    }

}

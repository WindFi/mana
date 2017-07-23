package me.sunzheng.mana;

import android.support.v4.content.ContextCompat;
import android.widget.TextView;

/**
 * Created by Sun on 2017/7/6.
 */

public class FavoriteCompact {

    public static void setFavorite(long status, TextView v) {
        // TODO: 2017/5/27 favior status
        String statusString = v.getResources().getStringArray(R.array.favorite_status_values)[(int) status];
        switch ((int) status) {
            case 1:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.favorite_wish));
                break;
            case 2:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.favorite_watched));
                break;
            case 3:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.favorite_watching));
                break;
            case 4:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.favorite_pause));
                break;
            case 5:
                v.setBackgroundColor(ContextCompat.getColor(v.getContext(), R.color.favorite_abanoned));
                break;
            default:
                break;
        }
        v.setText(statusString);
    }
}

package me.sunzheng.mana;

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
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            default:
                break;
        }
        v.setText(statusString);
    }
}

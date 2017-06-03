package me.sunzheng.mana.home;

import android.support.v7.widget.RecyclerView;

import me.sunzheng.mana.IPresenter;
import me.sunzheng.mana.IView;

/**
 * Created by Sun on 2017/5/23.
 */

public interface HomeContract {
    interface OnAir {
        interface View extends IView<Presenter> {
            void showProgressIntractor(boolean active);

            void setAdapter(RecyclerView.Adapter adapter);
        }

        interface Presenter extends IPresenter {
            void load(int type);
        }
    }

    interface Bangumi {

        interface View extends IView<Presenter> {
            void setWeekDay(long updateDate);

            void setAirDate(String startDate);

            void setSummary(CharSequence descript);

            void setOriginName(CharSequence originName);

            void setAdapter(RecyclerView.Adapter adapter);

            void setFaviorStatus(long status);

            void setName(CharSequence name);
        }

        interface Presenter extends IPresenter {
            void load(String id);
        }
    }
}

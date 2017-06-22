package me.sunzheng.mana.home;

import android.support.v7.widget.RecyclerView;

import java.util.List;

import me.sunzheng.mana.home.bangumi.wrapper.Episode;
import me.sunzheng.mana.utils.IPresenter;
import me.sunzheng.mana.utils.IView;

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
            void showProgressIntractor(boolean active);

            void setWeekDay(long updateDate);

            void setAirDate(String startDate);

            void setSummary(CharSequence descript);

            void setOriginName(CharSequence originName);

            void setEpisodes(List<Episode> episodeList);

            void setFaviorStatus(long status);

            void setName(CharSequence name);
        }

        interface Presenter extends IPresenter {
            void load(String id);
        }
    }

    interface Search {
        interface View extends IView<Presenter> {
            void showProgressIntractor(boolean active);

            void empty(String message);

            void setAdapter(RecyclerView.Adapter adapter);

            void notifyDataSetChanged();
        }

        interface Presenter extends IPresenter {
            void query(String key);

            void loadMore();
        }
    }
}

package me.sunzheng.mana.home;

import android.support.v7.widget.RecyclerView;

import java.util.List;

import me.sunzheng.mana.IPresenter;
import me.sunzheng.mana.IView;
import me.sunzheng.mana.home.bangumi.wrapper.Episode;

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
            void empty(String message);

            void setAdapter(RecyclerView.Adapter adapter);

            void notifyDataSetChanged();
        }

        interface Presenter extends IPresenter {
            void search(String key);

            void loadMore();
        }
    }
}

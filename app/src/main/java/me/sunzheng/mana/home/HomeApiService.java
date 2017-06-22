package me.sunzheng.mana.home;

import io.reactivex.Observable;
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper;
import me.sunzheng.mana.home.episode.wrapper.EpisodeWrapper;
import me.sunzheng.mana.home.mybangumi.wrapper.FaviourWrapper;
import me.sunzheng.mana.home.onair.wrapper.AirWrapper;
import me.sunzheng.mana.home.search.SearchResultWrapper;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by Sun on 2017/5/23.
 */

public interface HomeApiService {
    String HOME_PATH = "/api/home";

    interface MyBangumi {
        String PATH = HOME_PATH + "/my_bangumi";

        /**
         * list all bangumi which is favorite by current user and status is 3 (WATCHING)
         *
         * @return
         */
        @GET(PATH)
        Observable<FaviourWrapper> listMyBangumi();
    }

    interface OnAir {
        String PATH = HOME_PATH + "/on_air";

        /**
         * List all bangumi which is currently on air, on air means current date is between air_date and the last episode airdate plus one week.
         *
         * @param type 2:新番 6:日剧
         * @return
         */
        @GET(PATH)
        Observable<AirWrapper> listAll(@Query("type") int type);
    }

    interface Bangumi {
        String PATH = HOME_PATH + "/bangumi";

        /**
         * List all bangumi base on the given query criteria.
         *
         * @param page  the page number start from 1. Default: 1.
         * @param count count per page, by provide count = -1, client can obtain all data. Default: 10.
         * @param field the field name used for sort by. Default: air_date.
         * @param order the sort order which can be only two value: desc and asc. Default: desc.
         * @param name  the search term for filtering result. this string will be matched by name_cn, name and summary.
         * @return
         */
        @GET(PATH)
        Observable<SearchResultWrapper> listAll(@Query("page") int page, @Query("count") int count, @Query("sort_field") String field, @Query("sort_order") String order, @Query("name") String name);

        /**
         * Get the episode by specific id
         *
         * @param id
         * @return
         */
        @GET(PATH + "/{id}")
        Observable<BangumiDetailWrapper> getBangumiDetail(@Path("id") String id);
    }

    interface Episode {
        String PATH = HOME_PATH + "/episode";

        /**
         * Get the episode by specific id
         *
         * @param id
         * @return
         */
        @GET(PATH + "/{id}")
        Observable<EpisodeWrapper> getEpisode(@Path("id") String id);
    }

}

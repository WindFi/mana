package me.sunzheng.mana.home;

import io.reactivex.Observable;
import io.reactivex.Single;
import me.sunzheng.mana.home.bangumi.Response;
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper;
import me.sunzheng.mana.home.episode.wrapper.EpisodeWrapper;
import me.sunzheng.mana.home.mybangumi.wrapper.FavoriteWrapper;
import me.sunzheng.mana.home.onair.wrapper.AirWrapper;
import me.sunzheng.mana.home.search.SearchResultWrapper;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
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
         * the favorite status, can be one of the following value: 1 (WISH), 2 (WATCHED), 3 (WATCHING), 4 (PAUSE), 5 (ABANDONED) and 0 (all of above) Default: 3
         *
         * @return
         */
        @GET(PATH)
        Single<FavoriteWrapper> listMyBangumi(@Query("status") int status);
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
        Single<AirWrapper> listAll(@Query("type") int type);
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

        /**
         * @param id
         * @param request
         * @return
         */
        @POST("/api/watch/favorite/bangumi/{id}")
        Single<Response> changeBangumiFavoriteStatus(@Path("id") String id, @Body FavoriteStatusRequest request);
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

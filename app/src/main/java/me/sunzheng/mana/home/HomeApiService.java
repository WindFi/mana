package me.sunzheng.mana.home;

import io.reactivex.Observable;
import me.sunzheng.mana.home.bangumi.wrapper.BangumiDetailWrapper;
import me.sunzheng.mana.home.episode.wrapper.EpisodeWrapper;
import me.sunzheng.mana.home.onair.wrapper.AirWrapper;
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
        Observable<String> listMyBangumi();
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
         * @param page
         * @param count
         * @param field
         * @param order
         * @param name
         * @return
         */
        @GET(PATH)
        Observable<String> listAll(@Query("page") int page, @Query("count") int count, @Query("sort_field") String field, @Query("sort_order") String order, @Query("name") String name);

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

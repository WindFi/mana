package me.sunzheng.mana.account;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by Sun on 2017/5/22.
 */

public interface AccountApiService {

    interface Login {
        @POST("/api/user/login")
        Observable<LoginResponse> login(@Body LoginRequest request);
    }

    interface Logout {
        @POST("/api/user/logout")
        Observable<LoginResponse> logout();
    }

    interface Register {
        @POST("/api/user/register")
        Observable<LoginResponse> register(@Body RegisterRequest request);

    }


}

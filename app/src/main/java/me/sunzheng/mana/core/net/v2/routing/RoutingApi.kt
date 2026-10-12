package me.sunzheng.mana.core.net.v2.routing

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * mira routing API v1（契约见需求文档 §4.1）。
 *
 * 使用 suspend + retrofit2.Response（而不是项目里常见的 LiveData 适配器），因为客户端必须按 HTTP 状态码分支（见 §4.2）。
 */
interface RoutingApi {

    @GET("/_mira/routing/v1/backends")
    suspend fun backends(): Response<BackendsResponse>

    @POST("/_mira/routing/v1/routes")
    suspend fun createRoute(@Body body: RouteRequest): Response<RouteResponse>

    companion object {
        const val PATH_BACKENDS = "/_mira/routing/v1/backends"
        const val PATH_ROUTES = "/_mira/routing/v1/routes"
    }
}

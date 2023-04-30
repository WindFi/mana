package me.sunzheng.mana.home.bangumi

/**
 * Created by Sun on 2023/4/4.
 */

data class Response(
    var message: String? = null,
    var status: Long = 0
)

data class WatchProgressResponse(
    var message: String? = null,
    var status: Long = 0,
    var id: String? = null
)

data class DefaultResponse(
    var msg: String? = null
)
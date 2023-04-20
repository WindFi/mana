package me.sunzheng.mana.home.feedback

/**
 * Created by Sun on 2018/2/23.
 */
data class FeedbackRequestWrapper(
    var episode_id: String? = null,
    var video_file_id: String? = null,
    var message: String? = null
)
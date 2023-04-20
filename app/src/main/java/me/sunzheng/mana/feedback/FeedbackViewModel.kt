package me.sunzheng.mana.feedback

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import me.sunzheng.mana.core.net.ApiResponse
import me.sunzheng.mana.core.net.v2.ApiService
import me.sunzheng.mana.core.net.v2.NetworkBoundResource
import me.sunzheng.mana.home.bangumi.Response
import me.sunzheng.mana.home.feedback.FeedbackRequestWrapper
import javax.inject.Inject

class FeedbackViewModel : ViewModel() {
    @Inject
    lateinit var apiService: ApiService

    @Inject()
    lateinit var userName: String
    private var content: String? = null
    fun sendFeedback(message: String?) {
        content = message
    }

    fun submit(
        episodeId: String? = null,
        videoFileId: String? = null
    ) = object : NetworkBoundResource<Response, Response>() {
        var result: Response = Response("", -1)

        override fun saveCallResult(item: Response) {
            result = item
        }

        override fun shouldFetch(data: Response?) = true

        override fun loadFromDb() = liveData { emit(result) }

        override fun createCall(): LiveData<ApiResponse<Response>> = apiService.postFeedBack(
            FeedbackRequestWrapper(
                episode_id = episodeId,
                video_file_id = videoFileId,
                message = content
            )
        )
    }.asLiveData()
}
package me.sunzheng.mana.core.net

enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}

data class Resource<T>(val code: Status, val message: String?, val data: T?) {
    companion object {
        @JvmStatic
        fun <T> success(data: T?) = Resource(Status.SUCCESS, data = data, message = null)

        @JvmStatic
        fun <T> error(message: String, data: T?) =
            Resource(Status.ERROR, data = data, message = message)

        @JvmStatic
        fun <T> loading(data: T?) = Resource(Status.LOADING, data = data, message = null)

        @JvmStatic
        fun <T, M> switchMap(data: Resource<M>, block: (d: M?) -> T) =
            Resource(data.code, data.message, block(data.data))
    }
}
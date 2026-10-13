package me.sunzheng.mana.account.auth

import android.os.Parcel
import android.os.Parcelable
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.net.HttpCookie



/**
 * 逐字段 JSON 数组（design D2）。
 *
 * 为什么不用 `Parcel + Base64`：不可读、跨版本脆弱、无法做字段级演进。
 *
 * 关于 `HttpOnly`：`java.net.HttpCookie` **没有** httpOnly 字段（`HttpCookie.parse` 直接丢弃该属性，
 * Android 与 OpenJDK 实现一致），所以往返里它既不会保留也不会丢失 —— 改造前的 `Parcel` 编码同样没有它。
 * 本次纳入往返的属性是：name / value / domain / path / maxAge（过期时间）/ secure / comment / commentURL /
 * portlist / discard / version。
 */
object JsonCookieCodec {

    /** 序列化结构的版本号，落账号库 `userData.schema_version`（见 [AccountManagerSessionStore]）。 */
    const val SCHEMA_VERSION = "1"

    private val gson = Gson()

    fun encode(cookies: List<HttpCookie>): String =
        gson.toJson(cookies.map { CookieValueDto.from(it) })

    fun decode(value: String?): List<HttpCookie> {
        if (value.isNullOrEmpty()) {
            return emptyList()
        }
        return try {
            val parsed = gson.fromJson(value, Array<CookieValueDto>::class.java) ?: return emptyList()
            parsed.mapNotNull { it.toCookie() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/** [JsonCookieCodec] 的持久化结构；字段全部可空以便从任何历史版本反序列化。 */
internal data class CookieValueDto(
    val name: String? = null,
    val value: String? = null,
    val domain: String? = null,
    val path: String? = null,
    val comment: String? = null,
    val commentURL: String? = null,
    val portlist: String? = null,
    val maxAge: Long = -1L,
    val secure: Boolean = false,
    val discard: Boolean = false,
    val version: Int = 0
) {
    fun toCookie(): HttpCookie? {
        val cookieName = name ?: return null
        val cookie = HttpCookie(cookieName, value ?: "")
        // 字符串属性为 null 时保持 HttpCookie 的默认值（同样是 null），避免各实现 setXxx(null) 行为差异。
        domain?.let { cookie.domain = it }
        path?.let { cookie.path = it }
        comment?.let { cookie.comment = it }
        commentURL?.let { cookie.commentURL = it }
        portlist?.let { cookie.portlist = it }
        cookie.maxAge = maxAge
        cookie.secure = secure
        cookie.discard = discard
        cookie.version = version
        return cookie
    }

    companion object {
        fun from(cookie: HttpCookie): CookieValueDto = CookieValueDto(
            name = cookie.name,
            value = cookie.value,
            domain = cookie.domain,
            path = cookie.path,
            comment = cookie.comment,
            commentURL = cookie.commentURL,
            portlist = cookie.portlist,
            maxAge = cookie.maxAge,
            secure = cookie.secure,
            discard = cookie.discard,
            version = cookie.version
        )
    }
}

/**
 * 逐字段等价比较（`HttpCookie.equals` 只比 name/value/domain/path，不足以做迁移校验）。
 */
internal fun equivalentCookies(expected: List<HttpCookie>, actual: List<HttpCookie>): Boolean {
    if (expected.size != actual.size) {
        return false
    }
    val remaining = ArrayList(actual)
    for (cookie in expected) {
        val index = remaining.indexOfFirst { sameCookie(cookie, it) }
        if (index < 0) {
            return false
        }
        remaining.removeAt(index)
    }
    return true
}

private fun sameCookie(a: HttpCookie, b: HttpCookie): Boolean =
    a.name == b.name &&
        a.value == b.value &&
        a.domain == b.domain &&
        a.path == b.path &&
        a.maxAge == b.maxAge &&
        a.secure == b.secure &&
        a.version == b.version &&
        a.comment == b.comment &&
        a.commentURL == b.commentURL &&
        a.portlist == b.portlist &&
        a.discard == b.discard



/**
 * 「一个账号装多个 origin 凭据」的编解码（design D2 2026-10-12 调整）。
 *
 * 结构：`{"<origin>": [cookie, …], …}` —— 外层是 origin 键，内层复用 [JsonCookieCodec] 的 cookie 数组结构
 * （所以 cookie 属性映射只有一份实现、只有一份单测）。
 *
 * 为什么要有它：系统「设置 → 账号」里每行显示的就是 `account.name`（AOSP Settings 的
 * `AccountPreference` 里 `setTitle(mAccount.name)`），平台没有「每个账号一个显示名」的字段。
 * 因此要让那一行显示 App 名，只能把"一 origin 一账号"改成"一 App 一账号"，凭据按 origin 分槽存在包里。
 */
object JsonSessionBundleCodec {

    /** 凭据包结构的版本号，落账号库 `userData.schema_version`。 */
    const val SCHEMA_VERSION = "2"

    private val gson = Gson()

    fun encode(bundle: Map<String, List<HttpCookie>>): String {
        val root = JsonObject()
        for ((origin, cookies) in bundle) {
            root.add(origin, gson.fromJson(JsonCookieCodec.encode(cookies), JsonArray::class.java))
        }
        return root.toString()
    }

    /**
     * 解析凭据包。
     *
     * 返回值区分「空包」与「解析失败」——写路径必须靠它避免**用空包覆盖其它 origin 的凭据**：
     * - 入参为空 ⇒ 空 map（合法：还没有任何凭据）；
     * - 坏 JSON / 不是对象（例如老版本的单 origin 数组格式）⇒ `null`（调用方应放弃写入）；
     * - 对象 ⇒ 逐 origin 解析；值不是数组的条目跳过，单个 origin 里的坏 cookie 由 [JsonCookieCodec] 丢弃。
     */
    fun decode(value: String?): Map<String, List<HttpCookie>>? {
        if (value.isNullOrEmpty()) {
            return emptyMap()
        }
        return try {
            // 不是对象（坏 JSON / 老版本的单 origin 数组格式）会抛 JsonSyntaxException ⇒ null
            val root = gson.fromJson(value, JsonObject::class.java) ?: return null
            val result = LinkedHashMap<String, List<HttpCookie>>()
            for (origin in root.keySet()) {
                val cookies = root.get(origin)
                if (cookies == null || !cookies.isJsonArray) {
                    continue
                }
                result[origin] = JsonCookieCodec.decode(cookies.toString())
            }
            result
        } catch (e: Exception) {
            null
        }
    }
}



/**
 * 改造前 `PersistentHttpCookieStore` 使用的 `Parcel + Base64` 编码，**原样保留用于读老数据**。
 *
 * 升级用户 SP（`CookiePrefsFile`）里的值仍是这个格式（一个 cookie 一个键、一个 base64 值）；
 * [CompatCookieCodec] 先按 JSON 解，解不出来再回退到这里，从而保证「升级不掉登录」。
 *
 * 注意：`android.os.Parcel` / `android.util.Base64` 在纯 JVM 单测里是 stub，因此本类**不做 JVM 单测**，
 * 其正确性由「老数据格式逐字未改 + 真机升级路径验收」共同覆盖。
 */
private object LegacyParcelCookieCodec {

    /** 解一条老格式的 cookie 值；不可解析时返回 null（按「读不到」处理）。 */
    fun decodeOne(value: String?): HttpCookie? {
        if (value.isNullOrEmpty()) {
            return null
        }
        return try {
            val bytes = Base64.decode(value, Base64.DEFAULT)
            val parcel = Parcel.obtain()
            parcel.unmarshall(bytes, 0, bytes.size)
            parcel.setDataPosition(0)
            HttpCookieParcelable.CREATOR.createFromParcel(parcel).httpCookie
        } catch (e: RuntimeException) {
            // 脏值/半截写入按「读不到」处理，交给调用方降级，不打断请求链路。
            null
        }
    }

    /** 生成一条老格式的值；仅供兼容性验证使用，生产写路径统一走 JSON。 */
    fun encodeOne(cookie: HttpCookie): String {
        val parcel = Parcel.obtain()
        HttpCookieParcelable(cookie).writeToParcel(parcel, 0)
        val bytes = parcel.marshall()
        parcel.recycle()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private class HttpCookieParcelable(val httpCookie: HttpCookie) : Parcelable {

        private constructor(source: Parcel) : this(
            HttpCookie(source.readString(), source.readString()).apply {
                comment = source.readString()
                commentURL = source.readString()
                discard = source.readByte() != (0.toByte())
                domain = source.readString()
                maxAge = source.readLong()
                path = source.readString()
                portlist = source.readString()
                secure = source.readByte() != (0.toByte())
                version = source.readInt()
            }
        )

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            // 顺序必须与改造前逐字一致，否则老数据解不出来。
            dest.writeString(httpCookie.name)
            dest.writeString(httpCookie.value)
            dest.writeString(httpCookie.comment)
            dest.writeString(httpCookie.commentURL)
            dest.writeByte((if (httpCookie.discard) 1 else 0).toByte())
            dest.writeString(httpCookie.domain)
            dest.writeLong(httpCookie.maxAge)
            dest.writeString(httpCookie.path)
            dest.writeString(httpCookie.portlist)
            dest.writeByte((if (httpCookie.secure) 1 else 0).toByte())
            dest.writeInt(httpCookie.version)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<HttpCookieParcelable> =
                object : Parcelable.Creator<HttpCookieParcelable> {
                    override fun createFromParcel(source: Parcel): HttpCookieParcelable =
                        HttpCookieParcelable(source)

                    override fun newArray(size: Int): Array<HttpCookieParcelable?> =
                        arrayOfNulls(size)
                }
        }
    }
}

/**
 * 生产读路径使用的编解码：**写 JSON、读 JSON→老格式兼容**。
 * 这样升级用户的老值能读出来，之后任一写操作都会把它就地转成 JSON。
 */
object CompatCookieCodec {

    fun encode(cookies: List<HttpCookie>): String = JsonCookieCodec.encode(cookies)

    fun decode(value: String?): List<HttpCookie> {
        if (value.isNullOrEmpty()) {
            return emptyList()
        }
        val json = JsonCookieCodec.decode(value)
        if (json.isNotEmpty()) {
            return json
        }
        // 本变更写出的 JSON 一定以 '[' 开头：以此区分「JSON 解不出来」与「改造前的老格式」。
        if (value.trimStart().startsWith("[")) {
            return emptyList()
        }
        return LegacyParcelCookieCodec.decodeOne(value)?.let { listOf(it) } ?: emptyList()
    }
}


package me.sunzheng.mana.core.net.v2.routing

/**
 * 节点路由相关的功能日志统一 TAG（需求文档 §13.2）。
 * 抓取：`adb logcat -s PlaybackRouting:D`
 */
const val ROUTING_LOG_TAG = "PlaybackRouting"

/**
 * 纯逻辑：canonical URL 推导 + mira routing 客户端红线校验（需求文档 §4.3、§8.1）。
 *
 * 本文件**不允许**依赖任何 Android API（`android.net.Uri` 等），否则 JVM 单测无法运行；
 * 解析用 java.net 之外的纯字符串实现，非法输入一律返回 null / false，绝不抛异常。
 */
object RoutingResolver {

    /** mira 追加的路由查询参数名。 */
    const val ROUTE_QUERY_NAME = "__mira_route"

    /**
     * 由 host + 相对/绝对 url 推导 canonical 绝对 URL（语义对齐 HostUtil.makeUp）。
     *
     * - url 已是绝对 URL（含 scheme+authority，或 `//authority/...`）→ 原样返回，不被 host 改写；
     * - host 空白 + 相对 url → 返回 null（HostUtil.makeUp 此处会 `!!`/拼出相对路径，本需求范围内做 guard，见 §11）。
     */
    fun canonicalUrlOf(host: String?, url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (isAbsoluteUrl(url)) return url
        if (host.isNullOrBlank()) return null
        val base = if (host.endsWith("/")) host.substring(0, host.length - 1) else host
        return base + url
    }

    /** 从 canonical 绝对 URL 推导 origin（`scheme://authority`，小写）；非 http(s) 或非法 → null。 */
    fun originOf(canonicalUrl: String?): String? {
        val parts = parts(canonicalUrl) ?: return null
        val scheme = parts.scheme.lowercase()
        if (scheme != "http" && scheme != "https") return null
        return "$scheme://${parts.authority.lowercase()}"
    }

    /** path + query（不含 scheme/host）；非法 → null。 */
    fun resourceOf(canonicalUrl: String?): String? {
        val parts = parts(canonicalUrl) ?: return null
        val path = parts.rawPath.ifEmpty { "/" }
        val query = parts.rawQuery
        return if (query.isNullOrEmpty()) path else "$path?$query"
    }

    /**
     * 校验服务端返回的 playbackUrl（§4.3 红线 3，三项全满足才可用）：
     * 1. origin 与 canonical 相同；2. escaped path 与 canonical 相同；
     * 3. raw query == canonical 的 raw query 后面**恰好追加一个** `__mira_route=<routeToken>`。
     */
    fun isValidPlaybackUrl(
        canonicalUrl: String,
        routeToken: String,
        playbackUrl: String,
        routeQueryName: String = ROUTE_QUERY_NAME
    ): Boolean {
        if (routeToken.isBlank()) return false
        val canonical = parts(canonicalUrl) ?: return false
        val playback = parts(playbackUrl) ?: return false
        if (originOf(canonicalUrl) != originOf(playbackUrl)) return false
        if (canonical.rawPath != playback.rawPath) return false
        val expected = appendQuery(canonical.rawQuery, routeQueryName, routeToken)
        return playback.rawQuery == expected
    }

    /** 是否带有 `__mira_route` 参数（仅用于日志与缓存 key 判断，不打印 URL）。 */
    fun hasRouteParam(url: String?): Boolean {
        val query = queryOf(url) ?: return false
        return query.split('&').any { paramName(it) == ROUTE_QUERY_NAME }
    }

    /**
     * 生成媒体缓存 key 用的归一化 URL（FR-9）：剔除 `__mira_route` 参数，
     * 让 canonical 与各节点的签名地址共享同一份缓存。只剥这一个参数，其余原样保留。
     */
    fun normalizedCacheKey(url: String): String {
        val q = url.indexOf('?')
        if (q < 0) return url
        val hashIndex = url.indexOf('#', q)
        val head = if (hashIndex >= 0) url.substring(0, hashIndex) else url
        val fragment = if (hashIndex >= 0) url.substring(hashIndex) else ""
        val base = head.substring(0, q)
        val query = head.substring(q + 1)
        if (query.isEmpty()) return url

        var removed = false
        val kept = ArrayList<String>()
        for (part in query.split('&')) {
            if (paramName(part) == ROUTE_QUERY_NAME) removed = true else kept.add(part)
        }
        if (!removed) return url
        val newQuery = kept.joinToString("&")
        return if (newQuery.isEmpty()) base + fragment else "$base?$newQuery$fragment"
    }

    // ---------------------------------------------------------------- 内部实现

    private fun appendQuery(baseQuery: String?, name: String, value: String): String =
        if (baseQuery.isNullOrEmpty()) "$name=$value" else "$baseQuery&$name=$value"

    private fun paramName(part: String): String = part.substringBefore('=')

    private fun queryOf(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        val q = url.indexOf('?')
        if (q < 0) return null
        val hashIndex = url.indexOf('#', q)
        return if (hashIndex >= 0) url.substring(q + 1, hashIndex) else url.substring(q + 1)
    }

    private fun isAbsoluteUrl(url: String): Boolean {
        if (parts(url) != null) return true
        // 协议相对：//host/path —— android.net.Uri 会解析出 host，HostUtil.makeUp 也原样返回。
        return url.startsWith("//") && url.length > 2 && url[2] != '/'
    }

    private data class Parts(
        val scheme: String,
        val authority: String,
        val rawPath: String,
        val rawQuery: String?
    )

    private val SCHEME_PATTERN = Regex("[a-zA-Z][a-zA-Z0-9+.\\-]*")

    /** 纯字符串解析 `scheme://authority/path?query#fragment`；任何不合法形态返回 null。 */
    private fun parts(url: String?): Parts? {
        if (url.isNullOrEmpty()) return null
        val schemeEnd = url.indexOf("://")
        if (schemeEnd <= 0) return null
        val scheme = url.substring(0, schemeEnd)
        if (!SCHEME_PATTERN.matches(scheme)) return null

        val rest = url.substring(schemeEnd + 3)
        val authorityEnd = rest.indexOfFirst { it == '/' || it == '?' || it == '#' }
        val authority = if (authorityEnd < 0) rest else rest.substring(0, authorityEnd)
        if (authority.isEmpty()) return null

        var tail = if (authorityEnd < 0) "" else rest.substring(authorityEnd)
        val hashIndex = tail.indexOf('#')
        if (hashIndex >= 0) tail = tail.substring(0, hashIndex)
        val queryIndex = tail.indexOf('?')
        val rawPath = if (queryIndex < 0) tail else tail.substring(0, queryIndex)
        val rawQuery = if (queryIndex < 0) null else tail.substring(queryIndex + 1)
        return Parts(scheme, authority, rawPath, rawQuery)
    }
}

/** 路由解析结果（§8.1）。 */
sealed interface RouteResult {
    data class Routed(
        val canonicalUrl: String,
        val playbackUrl: String,
        val backend: BackendDto?
    ) : RouteResult

    data class Fallback(
        val canonicalUrl: String,
        val reason: String
    ) : RouteResult
}

package me.sunzheng.mana.core.net.v2.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [RoutingResolver] 的纯 JVM 用例（需求文档 §13.3）。
 * 该文件不依赖任何 Android API，可直接在 `./gradlew :app:testDebugUnitTest` 下运行。
 */
class RoutingResolverTest {

    private val host = "https://media.example.com"
    private val canonical = "https://media.example.com/video/example/file.mp4?quality=source"
    private val token = "payload.signature"
    private val playbackUrl = "$canonical&__mira_route=$token"

    // ------------------------------------------------------------ canonical URL

    @Test
    fun `relative url is completed with configured host`() {
        assertEquals(
            "https://media.example.com/video/example/file.mp4",
            RoutingResolver.canonicalUrlOf(host, "/video/example/file.mp4")
        )
    }

    @Test
    fun `host with trailing slash does not produce double slash`() {
        assertEquals(
            "https://media.example.com/video/example/file.mp4",
            RoutingResolver.canonicalUrlOf("$host/", "/video/example/file.mp4")
        )
    }

    @Test
    fun `absolute url is returned unchanged`() {
        val absolute = "http://other.example.org/a/b.mp4?x=1"
        assertEquals(absolute, RoutingResolver.canonicalUrlOf(host, absolute))
    }

    @Test
    fun `blank host with relative url returns null instead of crashing`() {
        assertNull(RoutingResolver.canonicalUrlOf("", "/video/example/file.mp4"))
        assertNull(RoutingResolver.canonicalUrlOf(null, "/video/example/file.mp4"))
        assertNull(RoutingResolver.canonicalUrlOf("   ", "/video/example/file.mp4"))
    }

    @Test
    fun `blank url returns null`() {
        assertNull(RoutingResolver.canonicalUrlOf(host, null))
        assertNull(RoutingResolver.canonicalUrlOf(host, ""))
    }

    // ------------------------------------------------------------ origin/resource

    @Test
    fun `origin is derived from canonical url`() {
        assertEquals("https://media.example.com", RoutingResolver.originOf(canonical))
        assertEquals("http://media.example.com:8080", RoutingResolver.originOf("http://media.example.com:8080/a.mp4"))
    }

    @Test
    fun `origin of non http url is null`() {
        assertNull(RoutingResolver.originOf("ftp://media.example.com/a.mp4"))
        assertNull(RoutingResolver.originOf("not-a-url"))
        assertNull(RoutingResolver.originOf(null))
    }

    @Test
    fun `resource keeps path and query without scheme or host`() {
        assertEquals(
            "/video/example/file.mp4?quality=source",
            RoutingResolver.resourceOf(canonical)
        )
    }

    @Test
    fun `resource without query is just the path`() {
        assertEquals("/video/example/file.mp4", RoutingResolver.resourceOf("$host/video/example/file.mp4"))
    }

    @Test
    fun `resource keeps escaped path untouched`() {
        assertEquals(
            "/video/%E4%B8%AD%E6%96%87/file.mp4?a=b",
            RoutingResolver.resourceOf("$host/video/%E4%B8%AD%E6%96%87/file.mp4?a=b")
        )
    }

    // ------------------------------------------------------------ playbackUrl check

    @Test
    fun `valid playback url passes all three checks`() {
        assertTrue(RoutingResolver.isValidPlaybackUrl(canonical, token, playbackUrl))
    }

    @Test
    fun `valid playback url without canonical query`() {
        val plain = "$host/video/file.mp4"
        assertTrue(
            RoutingResolver.isValidPlaybackUrl(plain, token, "$plain?__mira_route=$token")
        )
    }

    @Test
    fun `different origin is rejected`() {
        val other = "https://evil.example.com/video/example/file.mp4?quality=source&__mira_route=$token"
        assertFalse(RoutingResolver.isValidPlaybackUrl(canonical, token, other))
    }

    @Test
    fun `different path is rejected`() {
        val other = "https://media.example.com/video/example/other.mp4?quality=source&__mira_route=$token"
        assertFalse(RoutingResolver.isValidPlaybackUrl(canonical, token, other))
    }

    @Test
    fun `extra query parameter is rejected`() {
        val other = "$canonical&__mira_route=$token&extra=1"
        assertFalse(RoutingResolver.isValidPlaybackUrl(canonical, token, other))
    }

    @Test
    fun `mismatched token is rejected`() {
        val other = "$canonical&__mira_route=other.token"
        assertFalse(RoutingResolver.isValidPlaybackUrl(canonical, token, other))
    }

    @Test
    fun `missing route parameter is rejected`() {
        assertFalse(RoutingResolver.isValidPlaybackUrl(canonical, token, canonical))
    }

    @Test
    fun `blank token is rejected`() {
        assertFalse(RoutingResolver.isValidPlaybackUrl(canonical, "", "$canonical&__mira_route="))
    }

    // ------------------------------------------------------------ cache key (FR-9)

    @Test
    fun `route parameter is stripped from cache key`() {
        assertEquals(canonical, RoutingResolver.normalizedCacheKey(playbackUrl))
    }

    @Test
    fun `cache key keeps other query parameters in order`() {
        assertEquals(
            "$host/a.mp4?quality=source&lang=ja",
            RoutingResolver.normalizedCacheKey("$host/a.mp4?quality=source&__mira_route=$token&lang=ja")
        )
    }

    @Test
    fun `cache key of url without route parameter is unchanged`() {
        assertEquals(canonical, RoutingResolver.normalizedCacheKey(canonical))
    }

    @Test
    fun `cache key of route-only query falls back to bare path`() {
        assertEquals("$host/a.mp4", RoutingResolver.normalizedCacheKey("$host/a.mp4?__mira_route=$token"))
    }

    @Test
    fun `has route parameter detection`() {
        assertTrue(RoutingResolver.hasRouteParam(playbackUrl))
        assertFalse(RoutingResolver.hasRouteParam(canonical))
        assertFalse(RoutingResolver.hasRouteParam("$host/a.mp4?__mira_route_extra=1"))
        assertFalse(RoutingResolver.hasRouteParam(null))
    }
}

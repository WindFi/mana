package me.sunzheng.mana.account.auth

import java.net.HttpCookie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 凭据包编解码（design D2 2026-10-12 调整：一个账号装多个 origin）。
 *
 * 纯 JVM，无 Robolectric —— 与 `JsonCookieCodecTest` 同一套路。
 */
class SessionBundleCodecTest {

    private fun cookie(name: String, value: String, path: String = "/", maxAge: Long = 60L): HttpCookie {
        val cookie = HttpCookie(name, value)
        cookie.domain = "example.com"
        cookie.path = path
        cookie.maxAge = maxAge
        cookie.secure = true
        cookie.version = 1
        return cookie
    }

    @Test
    fun `round trip keeps every origin and cookie attribute`() {
        val bundle = mapOf(
            "http://example.com" to listOf(cookie("session", "a"), cookie("csrf", "b", path = "/api")),
            "https://other.example.com" to listOf(cookie("session", "c", maxAge = -1L))
        )

        val decoded = JsonSessionBundleCodec.decode(JsonSessionBundleCodec.encode(bundle))

        assertEquals(setOf("http://example.com", "https://other.example.com"), decoded?.keys)
        assertTrue(equivalentCookies(bundle.getValue("http://example.com"), decoded!!.getValue("http://example.com")))
        assertTrue(equivalentCookies(bundle.getValue("https://other.example.com"), decoded.getValue("https://other.example.com")))
    }

    @Test
    fun `unknown origin decodes to empty list`() {
        val encoded = JsonSessionBundleCodec.encode(mapOf("http://example.com" to listOf(cookie("session", "a"))))

        val decoded = JsonSessionBundleCodec.decode(encoded)

        assertTrue(decoded?.get("http://none.example.com").isNullOrEmpty())
    }

    @Test
    fun `empty or blank input decodes to an empty bundle`() {
        assertEquals(emptyMap<String, List<HttpCookie>>(), JsonSessionBundleCodec.decode(null))
        assertEquals(emptyMap<String, List<HttpCookie>>(), JsonSessionBundleCodec.decode(""))
        assertEquals(emptyMap<String, List<HttpCookie>>(), JsonSessionBundleCodec.decode("{}"))
    }

    @Test
    fun `legacy single origin array is not a bundle`() {
        // 老实现的 authToken 是裸数组（单 origin），不是包 —— 必须判为"不可用"，
        // 否则写路径会把它当空包，覆盖掉其中的凭据。
        assertNull(JsonSessionBundleCodec.decode(JsonCookieCodec.encode(listOf(cookie("session", "legacy")))))
    }

    @Test
    fun `garbage decodes to null so writers do not overwrite other origins`() {
        assertNull(JsonSessionBundleCodec.decode("not json at all"))
        assertNull(JsonSessionBundleCodec.decode("\"just a string\""))
    }

    @Test
    fun `entries whose value is not an array are skipped`() {
        val decoded = JsonSessionBundleCodec.decode(
            """{"http://example.com":[{"name":"session","value":"a"}],"http://bad.example.com":"oops"}"""
        )

        assertEquals(setOf("http://example.com"), decoded?.keys)
        assertEquals("a", decoded?.getValue("http://example.com")?.first()?.value)
    }
}

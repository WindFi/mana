package me.sunzheng.mana.account.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpCookie

/**
 * 任务 1.1：cookie ↔ 序列化值的往返编解码（design D2）。
 *
 * 覆盖：多 cookie 往返、属性不丢失、脏值/空值不抛异常。
 * 关于 `HttpOnly`：`java.net.HttpCookie` 没有该字段（解析时即被丢弃，改造前的 Parcel 编码同样没有），
 * 因此这里断言的是「HttpCookie 能表达的全部属性」。
 */
class JsonCookieCodecTest {

    @Test
    fun `round trip keeps every cookie attribute`() {
        val original = listOf(
            cookie("session", "uuid.signature"),
            cookie("theme", "dark", maxAge = 60L, secure = true)
        )
        val decoded = JsonCookieCodec.decode(JsonCookieCodec.encode(original))

        assertEquals(2, decoded.size)
        assertTrue(equivalentCookies(original, decoded))

        val session = decoded.first { it.name == "session" }
        assertEquals("uuid.signature", session.value)
        assertEquals("example.com", session.domain)
        assertEquals("/", session.path)
        assertEquals(2592000L, session.maxAge)
        assertEquals("comment", session.comment)
        assertEquals("https://example.com/comment", session.commentURL)
        assertEquals("80", session.portlist)
        assertTrue(session.discard)
        assertEquals(1, session.version)
        assertEquals(false, session.secure)

        val theme = decoded.first { it.name == "theme" }
        assertEquals(60L, theme.maxAge)
        assertEquals(true, theme.secure)
    }

    @Test
    fun `cookie names with dots and blanks survive`() {
        val original = listOf(cookie("a.b-c_d", ""), cookie("plain", "v"))
        val decoded = JsonCookieCodec.decode(JsonCookieCodec.encode(original))
        assertEquals(setOf("a.b-c_d", "plain"), decoded.map { it.name }.toSet())
    }

    @Test
    fun `encode of an empty list round trips to an empty list`() {
        assertEquals(emptyList<HttpCookie>(), JsonCookieCodec.decode(JsonCookieCodec.encode(emptyList())))
    }

    @Test
    fun `null and blank values decode to nothing`() {
        assertEquals(emptyList<HttpCookie>(), JsonCookieCodec.decode(null))
        assertEquals(emptyList<HttpCookie>(), JsonCookieCodec.decode(""))
    }

    @Test
    fun `malformed value decodes to nothing instead of throwing`() {
        assertEquals(emptyList<HttpCookie>(), JsonCookieCodec.decode("这不是 JSON"))
        assertEquals(emptyList<HttpCookie>(), JsonCookieCodec.decode("{not-an-array}"))
        assertEquals(emptyList<HttpCookie>(), JsonCookieCodec.decode("[{\"noName\":true}]"))
    }

    @Test
    fun `equivalence distinguishes a different value`() {
        val a = listOf(cookie("session", "v1"))
        val b = listOf(cookie("session", "v2"))
        assertTrue(!equivalentCookies(a, b))
        assertEquals(false, equivalentCookies(a, b))
    }
}

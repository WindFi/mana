package me.sunzheng.mana.account.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.URI

/**
 * 任务 1.1：origin 归一化 —— 必须与改造前 `PersistentHttpCookieStore.cookiesUri()` 逐字一致
 * （强制 http、丢弃端口、丢弃 path），否则账号库键与老 SP 键对不上，升级会掉登录。
 */
class SessionOriginTest {

    @Test
    fun `https request uri is normalized to http host`() {
        assertEquals("http://example.com", SessionOrigin.key(URI("https://example.com/api/home/on_air")))
    }

    @Test
    fun `port and path are dropped`() {
        assertEquals("http://example.com", SessionOrigin.key(URI("https://example.com:8443/a/b?c=d")))
    }

    @Test
    fun `host only uri stays the same key`() {
        assertEquals("http://example.com", SessionOrigin.key(URI("http://example.com")))
    }

    @Test
    fun `null uri has no key`() {
        assertNull(SessionOrigin.key(null))
        assertNull(SessionOrigin.normalize(null))
    }

    @Test
    fun `configured host url is normalized like a request uri`() {
        assertEquals("http://example.com", SessionOrigin.keyOfHost("https://example.com/"))
        assertEquals("http://example.com", SessionOrigin.keyOfHost("https://example.com"))
        assertEquals("http://example.com", SessionOrigin.keyOfHost("  https://example.com/api  "))
    }

    @Test
    fun `blank or hostless values have no origin`() {
        assertNull(SessionOrigin.keyOfHost(null))
        assertNull(SessionOrigin.keyOfHost(""))
        assertNull(SessionOrigin.keyOfHost("   "))
        assertNull(SessionOrigin.keyOfHost("example.com"))
        assertNull(SessionOrigin.keyOfHost("not a url"))
    }
}

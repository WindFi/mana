package me.sunzheng.mana.account.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

/**
 * 任务 1.2：`PersistentHttpCookieStore` 的匹配/过期/清理语义必须与改造前一致，只把持久化换成 [me.sunzheng.mana.account.auth.SessionStore]。
 */
class PersistentHttpCookieStoreTest {

    private val addUri = URI("https://example.com/api/user/login")
    private val requestUri = URI("https://example.com/api/home/my_bangumi")
    private val origin = "http://example.com"

    private fun store(sessionStore: FakeSessionStore = FakeSessionStore()) =
        sessionStore to PersistentHttpCookieStore(sessionStore)

    @Test
    fun `cookies are persisted under the normalized origin`() {
        val (backend, cookies) = store()
        cookies.add(addUri, cookie("session", "v1"))

        assertEquals(listOf(URI.create(origin)), cookies.getURIs())
        assertTrue(backend.contains(origin))
        assertEquals("v1", backend.read(origin).single().value)
    }

    @Test
    fun `domain matching still returns the cookie for another path of the same host`() {
        val (_, cookies) = store()
        cookies.add(addUri, cookie("session", "v1"))

        val forRequest = cookies.get(requestUri)
        assertEquals(listOf("session"), forRequest.map { it.name })
    }

    @Test
    fun `expired cookies are filtered out and never served`() {
        val (_, cookies) = store()
        cookies.add(addUri, cookie("session", "v1", maxAge = 0L))

        assertTrue(cookies.get(requestUri).isEmpty())
        assertTrue(cookies.getCookies().isEmpty())
    }

    @Test
    fun `add replaces an existing cookie of the same name`() {
        val (backend, cookies) = store()
        cookies.add(addUri, cookie("session", "v1"))
        cookies.add(addUri, cookie("session", "v2"))

        assertEquals(1, backend.read(origin).size)
        assertEquals("v2", cookies.get(requestUri).single().value)
    }

    @Test
    fun `remove deletes the cookie from memory and the backend`() {
        val (backend, cookies) = store()
        val session = cookie("session", "v1")
        cookies.add(addUri, session)

        assertTrue(cookies.remove(addUri, session))
        assertTrue(cookies.get(requestUri).isEmpty())
        assertFalse(backend.contains(origin))
    }

    @Test
    fun `removeAll clears memory and the backend`() {
        val (backend, cookies) = store()
        cookies.add(addUri, cookie("session", "v1"))

        assertTrue(cookies.removeAll())
        assertTrue(cookies.getURIs().isEmpty())
        assertFalse(backend.contains(origin))
    }

    @Test
    fun `clearForOrigin clears memory and the backend without touching other origins`() {
        val (backend, cookies) = store()
        cookies.add(addUri, cookie("session", "v1"))
        cookies.add(URI("https://other.example.com/a"), cookie("session", "other", domain = "other.example.com"))

        assertTrue(cookies.clearForOrigin(origin))

        assertTrue(cookies.get(requestUri).isEmpty())
        assertFalse(backend.contains(origin))
        assertTrue(backend.contains("http://other.example.com"))
        assertEquals(1, cookies.get(URI("https://other.example.com/a")).size)
    }

    @Test
    fun `clearForOrigin is safe when nothing was loaded or stored`() {
        val (_, cookies) = store()
        assertFalse(cookies.clearForOrigin(origin))
        assertFalse(cookies.clearForOrigin(origin))
    }

    @Test
    fun `cookies already in the backend are loaded lazily on first use`() {
        val backend = FakeSessionStore(mutableMapOf(origin to listOf(cookie("session", "v1"))))
        val cookies = PersistentHttpCookieStore(backend)

        assertEquals(listOf("session"), cookies.get(requestUri).map { it.name })
    }
}

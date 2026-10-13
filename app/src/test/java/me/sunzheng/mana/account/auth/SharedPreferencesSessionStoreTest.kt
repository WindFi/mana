package me.sunzheng.mana.account.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

/**
 * 任务 1.2：SP 后端的键语义必须与改造前的 `CookiePrefsFile` **逐字一致**，否则升级用户读不到老数据：
 *
 * ```
 * url            -> Set<origin>
 * <origin>       -> Set<origin + cookieName>
 * <origin+name>  -> 值
 * ```
 */
class SharedPreferencesSessionStoreTest {

    private val origin = "http://example.com"

    @Test
    fun `write keeps the legacy key layout`() {
        val prefs = FakeSharedPreferences()
        val store = SharedPreferencesSessionStore(prefs)

        assertTrue(store.write(origin, listOf(cookie("session", "v1"))))

        assertEquals(setOf(origin), prefs.getStringSet(SharedPreferencesSessionStore.KEY_URI_INDEX, emptySet()))
        assertEquals(setOf("$origin" + "session"), prefs.getStringSet(origin, emptySet()))
        assertNotNull(prefs.getString("$origin" + "session", null))
    }

    @Test
    fun `read retrieves through the per-origin index`() {
        val prefs = FakeSharedPreferences()
        val store = SharedPreferencesSessionStore(prefs)
        store.write(origin, listOf(cookie("session", "v1"), cookie("theme", "dark")))

        val cookies = store.read(origin)
        assertEquals(setOf("session", "theme"), cookies.map { it.name }.toSet())
        assertTrue(equivalentCookies(listOf(cookie("session", "v1"), cookie("theme", "dark")), cookies))
    }

    @Test
    fun `origin key comes from the same normalization as request uris`() {
        val key = SessionOrigin.key(URI("https://example.com:8443/api/home/on_air"))!!
        assertEquals(origin, key)

        val prefs = FakeSharedPreferences()
        val store = SharedPreferencesSessionStore(prefs)
        store.write(key, listOf(cookie("session", "v1")))
        assertTrue(store.read(origin).isNotEmpty())
    }

    @Test
    fun `rewriting an origin drops cookies that no longer exist`() {
        val prefs = FakeSharedPreferences()
        val store = SharedPreferencesSessionStore(prefs)
        store.write(origin, listOf(cookie("session", "v1"), cookie("theme", "dark")))
        store.write(origin, listOf(cookie("session", "v2")))

        assertEquals(listOf("session"), store.read(origin).map { it.name })
        assertNull(prefs.getString("$origin" + "theme", null))
        assertEquals(setOf("$origin" + "session"), prefs.getStringSet(origin, emptySet()))
    }

    @Test
    fun `delete removes only the target origin`() {
        val prefs = FakeSharedPreferences()
        val store = SharedPreferencesSessionStore(prefs)
        store.write(origin, listOf(cookie("session", "v1")))
        store.write("http://other.example.com", listOf(cookie("session", "other")))

        store.delete(origin)

        assertTrue(store.read(origin).isEmpty())
        assertNull(prefs.getString("$origin" + "session", null))
        assertEquals(setOf("http://other.example.com"), store.origins())
        assertFalse(store.read("http://other.example.com").isEmpty())
    }

    @Test
    fun `write of an empty list behaves like delete`() {
        val prefs = FakeSharedPreferences()
        val store = SharedPreferencesSessionStore(prefs)
        store.write(origin, listOf(cookie("session", "v1")))

        assertTrue(store.write(origin, emptyList()))
        assertTrue(store.read(origin).isEmpty())
        assertTrue(store.origins().isEmpty())
    }

    @Test
    fun `read of an unknown origin is empty`() {
        val store = SharedPreferencesSessionStore(FakeSharedPreferences())
        assertTrue(store.read("http://nobody.example.com").isEmpty())
        assertTrue(store.origins().isEmpty())
    }

    @Test
    fun `clear wipes every origin`() {
        val prefs = FakeSharedPreferences()
        val store = SharedPreferencesSessionStore(prefs)
        store.write(origin, listOf(cookie("session", "v1")))
        store.write("http://other.example.com", listOf(cookie("session", "other")))

        store.clear()

        assertTrue(store.origins().isEmpty())
        assertTrue(prefs.getAll().isEmpty())
    }

    @Test
    fun `usable credential ignores expired cookies`() {
        val store = SharedPreferencesSessionStore(FakeSharedPreferences())
        store.write(origin, listOf(cookie("session", "v1", maxAge = 0L)))
        assertFalse(store.hasUsableCredential(origin))

        store.write(origin, listOf(cookie("session", "v1", maxAge = 3600L)))
        assertTrue(store.hasUsableCredential(origin))
    }
}

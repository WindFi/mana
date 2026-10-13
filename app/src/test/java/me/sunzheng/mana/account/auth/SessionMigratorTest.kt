package me.sunzheng.mana.account.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

/**
 * 任务 3.1/3.2：一次性迁移的决策表 —— 先校验、后清除。
 */
class SessionMigratorTest {

    private val origin = "http://example.com"
    private val cookies = listOf(cookie("session", "v1"))

    private fun migrator(
        target: SessionStore,
        legacy: SessionStore,
        warnings: MutableList<String> = ArrayList()
    ) = MigratingSessionStore(target, legacy) { warnings += it }

    @Test
    fun `migration moves the credential and clears the legacy copy only after verification`() {
        val target = FakeSessionStore()
        val legacy = FakeSessionStore(mutableMapOf(origin to cookies))

        assertEquals(MigrationOutcome.MOVED, migrator(target, legacy).migrate(origin))
        assertTrue(equivalentCookies(cookies, target.read(origin)))
        assertFalse(legacy.contains(origin))
    }

    @Test
    fun `target already having a credential skips migration and keeps the legacy copy`() {
        val target = FakeSessionStore(mutableMapOf(origin to cookies))
        val legacy = FakeSessionStore(mutableMapOf(origin to cookies))

        assertEquals(MigrationOutcome.TARGET_PRESENT, migrator(target, legacy).migrate(origin))
        assertTrue(legacy.contains(origin))
    }

    @Test
    fun `both sides empty is source empty`() {
        val target = FakeSessionStore()
        val legacy = FakeSessionStore()

        assertEquals(MigrationOutcome.SOURCE_EMPTY, migrator(target, legacy).migrate(origin))
        assertFalse(target.contains(origin))
    }

    @Test
    fun `write failure keeps the legacy copy`() {
        val target = FakeSessionStore(failWrite = true)
        val legacy = FakeSessionStore(mutableMapOf(origin to cookies))
        val warnings = ArrayList<String>()

        assertEquals(MigrationOutcome.WRITE_FAILED, migrator(target, legacy, warnings).migrate(origin))
        assertTrue(legacy.contains(origin))
        assertEquals(1, warnings.size)
    }

    @Test
    fun `read back mismatch keeps the legacy copy`() {
        val target = FakeSessionStore(corruptOnWrite = true)
        val legacy = FakeSessionStore(mutableMapOf(origin to cookies))
        val warnings = ArrayList<String>()

        assertEquals(MigrationOutcome.VERIFY_FAILED, migrator(target, legacy, warnings).migrate(origin))
        assertTrue(legacy.contains(origin))
        assertEquals(1, warnings.size)
    }

    @Test
    fun `only the migrated origin is cleared, other origins stay`() {
        val target = FakeSessionStore()
        val legacy = FakeSessionStore(
            mutableMapOf(
                origin to cookies,
                "http://other.example.com" to listOf(cookie("session", "other"))
            )
        )

        assertEquals(MigrationOutcome.MOVED, migrator(target, legacy).migrate(origin))
        assertFalse(legacy.contains(origin))
        assertTrue(legacy.contains("http://other.example.com"))
        assertEquals(setOf("http://other.example.com"), legacy.origins())
    }

    @Test
    fun `fallback store consults the primary first and then the legacy backend`() {
        val primary = FakeSessionStore()
        val legacy = FakeSessionStore(mutableMapOf(origin to cookies))
        val store = MigratingSessionStore(primary, legacy)

        assertTrue(store.hasUsableCredential(origin))
        assertTrue(equivalentCookies(cookies, store.read(origin)))
        assertFalse(legacy.contains(origin))
    }

    @Test
    fun `fallback store degrades to the legacy backend when the primary keeps failing`() {
        val primary = FakeSessionStore(failWrite = true, readFailure = true)
        val legacy = FakeSessionStore()
        val store = MigratingSessionStore(primary, legacy)

        assertTrue(store.write(origin, cookies))
        assertTrue(legacy.contains(origin))
        assertTrue(store.hasUsableCredential(origin))
    }

    @Test
    fun `fallback store migrates only once per origin per process`() {
        val primary = FakeSessionStore()
        val legacy = FakeSessionStore(mutableMapOf(origin to cookies))
        val migrator = migrator(primary, legacy)
        val store = MigratingSessionStore(primary, legacy)

        store.read(origin)
        val afterFirst = primary.writeCount
        store.read(origin)
        assertEquals(afterFirst, primary.writeCount)
    }

    @Test
    fun `delete clears both backends`() {
        val primary = FakeSessionStore(mutableMapOf(origin to cookies))
        val legacy = FakeSessionStore(mutableMapOf(origin to cookies))
        val store = MigratingSessionStore(primary, legacy)

        store.delete(origin)

        assertFalse(primary.contains(origin))
        assertFalse(legacy.contains(origin))
    }

    @Test
    fun `origins is the union of both backends`() {
        val primary = FakeSessionStore(mutableMapOf(origin to cookies))
        val legacy = FakeSessionStore(mutableMapOf("http://other.example.com" to cookies))
        val store = MigratingSessionStore(primary, legacy)

        assertEquals(setOf(origin, "http://other.example.com"), store.origins())
    }

    @Test
    fun `normalized keys line up between request uri and configured host`() {
        val requestKey = SessionOrigin.key(URI("https://example.com/api/user/login"))!!
        val hostKey = SessionOrigin.keyOfHost("https://example.com")!!
        assertEquals(requestKey, hostKey)
    }
}

package me.sunzheng.mana.account.auth

import me.sunzheng.mana.utils.PreferenceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

/**
 * 任务 4.1/4.3/5.1/5.4：登出（主动 + 系统侧删除共用的 `clearSession`）与启动核对的判定表。
 *
 * 用真 [PersistentHttpCookieStore] + 内存后端，覆盖「账号库/内存/SP 三处凭据都被清」与幂等性。
 */
class SessionLogoutTest {

    private val origin = "http://example.com"
    private val requestUri = URI("https://example.com/api/home/my_bangumi")

    private inner class Fixture(
        val store: FakeSessionStore = FakeSessionStore(),
        val prefs: FakeSharedPreferences = FakeSharedPreferences()
    ) {
        val cookieStore = PersistentHttpCookieStore(store)
        val logout = SessionLogout(prefs, store, cookieStore)

        fun loginWithRememberedCredential() {
            cookieStore.add(requestUri, cookie("session", "v1"))
            prefs.edit()
                .putBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, true)
                .putString(PreferenceManager.Global.STR_USERNAME, "sun")
                .putString(PreferenceManager.Global.STR_PASSWORD, "secret")
                .apply()
        }

        fun hasLoginFlags(): Boolean =
            prefs.getBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, false) ||
                !prefs.getString(PreferenceManager.Global.STR_USERNAME, "").isNullOrEmpty() ||
                !prefs.getString(PreferenceManager.Global.STR_PASSWORD, "").isNullOrEmpty()
    }

    @Test
    fun `logout clears credential in store, memory and login flags`() {
        val fixture = Fixture()
        fixture.loginWithRememberedCredential()
        assertTrue(fixture.store.contains(origin))
        assertTrue(fixture.cookieStore.get(requestUri).isNotEmpty())

        assertTrue(fixture.logout.clearSession(origin))

        assertFalse(fixture.store.contains(origin))
        assertTrue(fixture.cookieStore.get(requestUri).isEmpty())
        assertFalse(fixture.hasLoginFlags())
        assertFalse(fixture.store.hasUsableCredential(origin))
    }

    @Test
    fun `logout is idempotent`() {
        val fixture = Fixture()
        fixture.loginWithRememberedCredential()

        assertTrue(fixture.logout.clearSession(origin))
        assertFalse(fixture.logout.clearSession(origin))
        assertFalse(fixture.logout.clearSession(origin))
    }

    @Test
    fun `logout only affects the target origin`() {
        val fixture = Fixture()
        fixture.loginWithRememberedCredential()
        fixture.cookieStore.add(URI("https://other.example.com/a"), cookie("session", "other", domain = "other.example.com"))

        fixture.logout.clearSession(origin)

        assertFalse(fixture.store.contains(origin))
        assertTrue(fixture.store.contains("http://other.example.com"))
        assertTrue(fixture.cookieStore.get(URI("https://other.example.com/a")).isNotEmpty())
    }

    @Test
    fun `logout succeeds when the account no longer exists`() {
        val fixture = Fixture()
        fixture.prefs.edit().putBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, true).apply()

        // 账号库里的账号已经被用户在系统设置里删掉了：这里只有判据残留。
        assertTrue(fixture.logout.clearSession(origin))
        assertFalse(fixture.hasLoginFlags())
    }

    @Test
    fun `logout without an origin still clears the login flags`() {
        val fixture = Fixture()
        fixture.prefs.edit()
            .putBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, true)
            .putString(PreferenceManager.Global.STR_USERNAME, "sun")
            .apply()

        assertTrue(fixture.logout.clearSession(null))
        assertFalse(fixture.hasLoginFlags())
    }

    @Test
    fun `startup check keeps a usable credential and does not touch the flags`() {
        val fixture = Fixture()
        fixture.loginWithRememberedCredential()

        assertTrue(fixture.logout.verifyOnStartup(origin))
        assertTrue(fixture.store.contains(origin))
        assertTrue(fixture.hasLoginFlags())
    }

    @Test
    fun `startup check clears everything when the credential is gone`() {
        val fixture = Fixture()
        fixture.prefs.edit()
            .putBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, true)
            .putString(PreferenceManager.Global.STR_USERNAME, "sun")
            .putString(PreferenceManager.Global.STR_PASSWORD, "secret")
            .apply()

        assertFalse(fixture.logout.verifyOnStartup(origin))
        assertFalse(fixture.hasLoginFlags())
    }

    @Test
    fun `startup check clears an expired credential`() {
        val fixture = Fixture()
        fixture.store.write(origin, listOf(cookie("session", "v1", maxAge = 0L)))
        fixture.prefs.edit().putString(PreferenceManager.Global.STR_USERNAME, "sun").apply()

        assertFalse(fixture.logout.verifyOnStartup(origin))
        assertFalse(fixture.hasLoginFlags())
        assertFalse(fixture.store.contains(origin))
    }

    @Test
    fun `startup check treats a legacy only credential as logged in and migrates first`() {
        // 任务 5.4：升级用户（SP 有凭据、账号库为空）首次启动必须「先迁移后判据」，不能被登出。
        val legacyStore = FakeSessionStore(mutableMapOf(origin to listOf(cookie("session", "v1"))))
        val accountStore = FakeSessionStore()
        val migrator = MigratingSessionStore(accountStore, legacyStore) { }
        val store = MigratingSessionStore(accountStore, legacyStore)
        val prefs = FakeSharedPreferences()
        prefs.edit().putString(PreferenceManager.Global.STR_USERNAME, "sun").apply()
        val cookieStore = PersistentHttpCookieStore(store)
        val logout = SessionLogout(prefs, store, cookieStore)

        assertTrue(logout.verifyOnStartup(origin))
        assertTrue(accountStore.contains(origin))
        assertFalse(legacyStore.contains(origin))
        assertFalse(prefs.getString(PreferenceManager.Global.STR_USERNAME, "").isNullOrEmpty())
    }

    @Test
    fun `startup check does not resurrect a credential deleted while the app was off`() {
        // App 未运行时账号被删：存储层什么都没有，只有判据残留。
        val fixture = Fixture()
        fixture.prefs.edit()
            .putString(PreferenceManager.Global.STR_USERNAME, "sun")
            .putString(PreferenceManager.Global.STR_PASSWORD, "secret")
            .apply()

        assertFalse(fixture.logout.verifyOnStartup(origin))
        assertEquals("", fixture.prefs.getString(PreferenceManager.Global.STR_USERNAME, ""))
        assertEquals("", fixture.prefs.getString(PreferenceManager.Global.STR_PASSWORD, ""))
    }

    @Test
    fun `login state predicate follows credential availability not stored usernames`() {
        val fixture = Fixture()
        fixture.cookieStore.add(requestUri, cookie("session", "v1"))
        // 老判据依赖的用户名/密码为空，但凭据在 —— 必须判为已登录（任务 5.2）。
        assertFalse(fixture.hasLoginFlags())
        assertTrue(fixture.store.hasUsableCredential(origin))
    }

    @Test
    fun `account name is not an origin so per-origin clearing leaves the real credential behind`() {
        // 回归锁：把账号名（Mana）当 origin 传进来时，真实 origin 的凭据必须原样保留。
        val fixture = Fixture()
        fixture.loginWithRememberedCredential()

        fixture.logout.clearSession("Mana")

        assertTrue("真实 origin 的凭据不能被账号名清掉", fixture.store.contains(origin))
        assertTrue(fixture.cookieStore.get(requestUri).isNotEmpty())
    }

    @Test
    fun `clearAllSessions wipes memory, both backends and login flags`() {
        val fixture = Fixture()
        fixture.loginWithRememberedCredential()
        fixture.store.write(origin, listOf(cookie("session", "v1")))

        val cleared = fixture.logout.clearAllSessions()

        assertEquals(setOf(origin), cleared)
        assertTrue(fixture.store.read(origin).isEmpty())
        assertTrue(fixture.cookieStore.get(requestUri).isEmpty())
        assertFalse(fixture.hasLoginFlags())
    }

    @Test
    fun `clearAllSessions reports the snapshot taken before the account disappeared`() {
        // 真机实测：删除回调里账号已被平台删掉，此时读 origins() 必然为空 ⇒ 必须用删除前快照。
        val fixture = Fixture()
        fixture.store.write(origin, listOf(cookie("session", "v1")))
        val snapshot = fixture.logout.currentOrigins()
        fixture.store.clear() // 模拟"平台已经把账号删了"

        assertEquals(setOf(origin), snapshot)
        assertEquals(setOf(origin), fixture.logout.clearAllSessions(snapshot))
        assertTrue(fixture.logout.clearAllSessions().isEmpty()) // 现读仍为空，佐证必须传快照
    }

    @Test
    fun `clearAllSessions is safe and idempotent when nothing is stored`() {
        val fixture = Fixture()

        assertTrue(fixture.logout.clearAllSessions().isEmpty())
        assertTrue(fixture.logout.clearAllSessions().isEmpty())
        assertFalse(fixture.hasLoginFlags())
    }

    @Test
    fun `startup check keeps an origin whose stored value cannot be decoded`() {
        // design D13 结论 2：删除会销毁设备上唯一一份凭据，因此「键在、值解不出」必须保留原值。
        val fixture = Fixture(FakeSessionStore(undecodableOrigins = setOf(origin)))
        fixture.store.write(origin, listOf(cookie("session", "v1")))

        assertFalse(fixture.logout.verifyOnStartup(origin))

        assertTrue("解不出的原始数据必须保留，等待人工/后续版本处理", fixture.store.contains(origin))
    }

    @Test
    fun `startup check still clears when there is no raw data at all`() {
        // 与上一条对照：存储层没有任何原始数据（连键都没有）时，启动核对必须照旧清掉判据。
        val fixture = Fixture()
        fixture.prefs.edit()
            .putBoolean(PreferenceManager.Global.BOOL_IS_REMEMBERD, true)
            .apply()
        assertTrue(fixture.hasLoginFlags())

        assertFalse(fixture.logout.verifyOnStartup(origin))

        assertFalse(fixture.hasLoginFlags())
    }

    @Test
    fun `login state predicate degrades to the legacy backend when the account store fails`() {
        val legacyStore = FakeSessionStore(mutableMapOf(origin to listOf(cookie("session", "v1"))))
        val accountStore = FakeSessionStore(readFailure = true, failWrite = true)
        // 主后端（账号库）+ 兜底（SP）+ 迁移已合成一个类（2026-10-12 归集）。
        val store = MigratingSessionStore(accountStore, legacyStore) { }

        assertTrue(store.hasUsableCredential(origin))
    }
}

package me.sunzheng.mana.account.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 任务 2.1/2.4：后端选择、账号类型派生与账号库写入限制的**纯判定**表
 * （真机上的 AccountManager 行为另走验收清单）。
 */
class SessionBackendPolicyTest {

    @Test
    fun `account manager is used only from api 23 with an available backend`() {
        assertTrue(SessionBackendPolicy.useAccountManager(23, true))
        assertTrue(SessionBackendPolicy.useAccountManager(34, true))
        assertFalse(SessionBackendPolicy.useAccountManager(22, true))
        assertFalse(SessionBackendPolicy.useAccountManager(21, true))
        assertFalse(SessionBackendPolicy.useAccountManager(23, false))
        assertFalse(SessionBackendPolicy.useAccountManager(34, false))
    }

    @Test
    fun `account type derives from application id so debug and release never collide`() {
        assertEquals("me.sunzheng.mana.session", SessionBackendPolicy.accountTypeOf("me.sunzheng.mana"))
        assertEquals("me.sunzheng.mana.debug.session", SessionBackendPolicy.accountTypeOf("me.sunzheng.mana.debug"))
        assertFalse(
            SessionBackendPolicy.accountTypeOf("me.sunzheng.mana") ==
                SessionBackendPolicy.accountTypeOf("me.sunzheng.mana.debug")
        )
    }

    @Test
    fun `over long auth tokens must degrade to private storage`() {
        assertFalse(SessionBackendPolicy.canStoreAuthToken(null))
        assertFalse(SessionBackendPolicy.canStoreAuthToken(""))
        assertTrue(SessionBackendPolicy.canStoreAuthToken("x"))

        val atLimit = "x".repeat(SessionBackendPolicy.MAX_AUTH_TOKEN_LENGTH)
        assertTrue(SessionBackendPolicy.canStoreAuthToken(atLimit))
        assertFalse(SessionBackendPolicy.canStoreAuthToken(atLimit + "x"))
    }

    @Test
    fun `missing token means falling back to the legacy predicate`() {
        assertFalse(SessionBackendPolicy.hasUsableToken(null))
        assertFalse(SessionBackendPolicy.hasUsableToken(""))
        assertTrue(SessionBackendPolicy.hasUsableToken("uuid.signature"))
    }
}

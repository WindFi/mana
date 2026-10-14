package me.sunzheng.mana.account.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 任务 2.1/4.2：「系统侧删除账号 ⇒ 视为登出」的判定表（design D11）。
 *
 * 判定依据是**账号标识**（账号名 + 账号类型）而不是账号名；本 App 只有一个账号，
 * 因此「本 App 账号标识消失」等价于全部 origin 登出。其它应用/其它类型的账号一律不影响本 App。
 */
class SessionAccountEventsTest {

    private val accountType = "me.sunzheng.mana.session"
    private val mine = AccountKey("http://example.com", accountType)

    @Test
    fun `only accounts of our type count as ours`() {
        val current = listOf(
            mine,
            AccountKey("http://example.com", "com.other.app.session"),
            AccountKey("user@example.com", "com.google")
        )
        assertEquals(setOf(mine), ourAccounts(current, accountType))
    }

    @Test
    fun `an account that is still there needs no cleanup`() {
        assertFalse(
            ownAccountDisappeared(
                known = listOf(mine),
                current = listOf(mine),
                accountType = accountType
            )
        )
    }

    @Test
    fun `an account that disappeared is cleared`() {
        assertTrue(
            ownAccountDisappeared(
                known = listOf(mine),
                current = emptyList(),
                accountType = accountType
            )
        )
    }

    @Test
    fun `the first snapshot after startup is not a deletion`() {
        // 注册监听时会立刻回调一次启动快照；known 为空不得判定为「用户删除了账号」（否则启动即误清）。
        assertFalse(
            ownAccountDisappeared(
                known = emptyList(),
                current = emptyList(),
                accountType = accountType
            )
        )
        assertFalse(
            ownAccountDisappeared(
                known = emptyList(),
                current = listOf(mine),
                accountType = accountType
            )
        )
    }

    @Test
    fun `deleting another app's account does not touch ours`() {
        assertFalse(
            ownAccountDisappeared(
                known = listOf(AccountKey("user@example.com", "com.other.app.session")),
                current = emptyList(),
                accountType = accountType
            )
        )
        assertFalse(
            ownAccountDisappeared(
                known = listOf(mine),
                current = listOf(mine, AccountKey("http://other.example.com", "com.other.app.session")),
                accountType = accountType
            )
        )
    }

    @Test
    fun `a same-named account of another type does not mask our deletion`() {
        assertTrue(
            ownAccountDisappeared(
                known = listOf(AccountKey("Mana", accountType)),
                current = listOf(AccountKey("Mana", "com.other.app.session")),
                accountType = accountType
            )
        )
    }

    @Test
    fun `removing our only account while others remain is still a deletion`() {
        assertTrue(
            ownAccountDisappeared(
                known = listOf(AccountKey("Mana", accountType)),
                current = listOf(
                    AccountKey("user@example.com", "com.google"),
                    AccountKey("http://example.com", "com.other.app.session")
                ),
                accountType = accountType
            )
        )
    }
}

package me.sunzheng.mana.account.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 任务 4.2：「系统侧删除账号 ⇒ 视为登出」的判定表。
 *
 * 命中本 App accountType 且消失才清；其它 origin / 其它应用账号一律不受影响。
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
        assertEquals(setOf("http://example.com"), mineAccounts(current, accountType))
    }

    @Test
    fun `an account that is still there needs no cleanup`() {
        val toClear = originsToClear(
            knownMine = setOf("http://example.com"),
            current = listOf(mine),
            accountType = accountType
        )
        assertTrue(toClear.isEmpty())
    }

    @Test
    fun `an account that disappeared is cleared`() {
        val toClear = originsToClear(
            knownMine = setOf("http://example.com"),
            current = emptyList(),
            accountType = accountType
        )
        assertEquals(setOf("http://example.com"), toClear)
    }

    @Test
    fun `deleting another origin does not touch the current session`() {
        val toClear = originsToClear(
            knownMine = setOf("http://example.com"),
            current = listOf(mine),
            accountType = accountType
        )
        assertTrue(toClear.isEmpty())
    }

    @Test
    fun `deleting another app's account does not touch ours`() {
        val toClear = originsToClear(
            knownMine = setOf("http://example.com"),
            current = listOf(mine, AccountKey("http://other.example.com", "com.other.app.session")),
            accountType = accountType
        )
        assertTrue(toClear.isEmpty())
    }

    @Test
    fun `a same-named account of another type does not mask our deletion`() {
        val toClear = originsToClear(
            knownMine = setOf("http://example.com"),
            current = listOf(AccountKey("http://example.com", "com.other.app.session")),
            accountType = accountType
        )
        assertEquals(setOf("http://example.com"), toClear)
    }

    @Test
    fun `only the vanished origin of several is reported`() {
        val toClear = originsToClear(
            knownMine = setOf("http://a.example.com", "http://b.example.com"),
            current = listOf(AccountKey("http://b.example.com", accountType)),
            accountType = accountType
        )
        assertEquals(setOf("http://a.example.com"), toClear)
    }
}

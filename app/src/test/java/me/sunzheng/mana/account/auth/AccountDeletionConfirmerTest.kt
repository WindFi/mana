package me.sunzheng.mana.account.auth

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * §3.1：「账号消失 ⇒ 清理」的**去抖 + 复核**时序（design D13）。
 *
 * 背景（2026-10-14 真机复现后的定案）：账号库的写入方就是我们自己 —— 迁移过程中
 * `AccountManagerSessionStore` 会 `addAccountExplicitly`（新建单账号）与 `removeAccountExplicitly`
 * （清掉旧模型的同名 origin 账号），每个动作都会回调监听器，监听器可能看到**瞬态快照**。
 * 若收到"本 App 账号消失"立刻清理，会把刚迁好的凭据连账号一起清掉（升级用户掉登录的形态）。
 *
 * 这里用假调度器（同步立即执行）驱动，不引入真实等待。
 */
class AccountDeletionConfirmerTest {

    /** 假调度器：记录延迟，并在调用处同步执行 action（等价于"延迟到点"）。 */
    private class FakeScheduler {
        var scheduledDelayMs: Long? = null
        private var action: (() -> Unit)? = null

        fun schedule(delayMs: Long, action: () -> Unit) {
            scheduledDelayMs = delayMs
            this.action = action
        }

        /** 模拟延迟到期，执行排队的复核。 */
        fun runNow() = action!!.invoke()
    }

    @Test
    fun `迁移成功的过程中收到账号变更回调不得清空`() {
        // 迁移刚写完账号库：回调给的是"只有新账号 / 老账号已消失"的瞬态快照，但复核时账号仍在。
        val scheduler = FakeScheduler()
        var clearedCount = 0
        val confirmer = AccountDeletionConfirmer(
            recheckDelayMs = 400L,
            scheduleRecheck = scheduler::schedule,
            accountsOfOurType = { setOf("Mana") },
            onConfirmedDeletion = { clearedCount++ }
        )

        confirmer.onAccountDisappeared()

        assertEquals("必须先延迟再复核（去抖）", 400L, scheduler.scheduledDelayMs)
        assertEquals("延迟期间不得清理", 0, clearedCount)

        scheduler.runNow()

        assertEquals("复核到账号仍在（迁移刚建的账号）⇒ 不得清理", 0, clearedCount)
    }

    @Test
    fun `复核仍为空才清理`() {
        val scheduler = FakeScheduler()
        var clearedCount = 0
        val confirmer = AccountDeletionConfirmer(
            recheckDelayMs = 400L,
            scheduleRecheck = scheduler::schedule,
            accountsOfOurType = { emptySet() },
            onConfirmedDeletion = { clearedCount++ }
        )

        confirmer.onAccountDisappeared()
        assertEquals(0, clearedCount)
        scheduler.runNow()

        assertEquals("复核确认账号真的没了 ⇒ 清理一次", 1, clearedCount)
    }

    @Test
    fun `连续回调只排队一次复核`() {
        // 平台一次删除可能连发多个快照；不应堆叠出多次清理。
        val scheduler = FakeScheduler()
        var clearedCount = 0
        var recheckCount = 0
        val confirmer = AccountDeletionConfirmer(
            recheckDelayMs = 400L,
            scheduleRecheck = scheduler::schedule,
            accountsOfOurType = { recheckCount++; emptySet() },
            onConfirmedDeletion = { clearedCount++ }
        )

        confirmer.onAccountDisappeared()
        confirmer.onAccountDisappeared()
        confirmer.onAccountDisappeared()

        assertEquals("排队期间只复核一次", 0, recheckCount)
        scheduler.runNow()
        assertEquals("复核一次", 1, recheckCount)
        assertEquals("只清理一次", 1, clearedCount)
    }

    @Test
    fun `确认过一轮之后再次消失仍会复核`() {
        // pending 必须在复核后复位，否则第二次删除会被永久忽略。
        val scheduler = FakeScheduler()
        var clearedCount = 0
        var accountExists = false
        val confirmer = AccountDeletionConfirmer(
            recheckDelayMs = 400L,
            scheduleRecheck = scheduler::schedule,
            accountsOfOurType = { if (accountExists) setOf("Mana") else emptySet() },
            onConfirmedDeletion = { clearedCount++ }
        )

        confirmer.onAccountDisappeared()
        scheduler.runNow()
        assertEquals(1, clearedCount)

        // 用户重新登录（迁移又建了账号）后又删掉账号：第二轮仍要复核并清理。
        accountExists = true
        confirmer.onAccountDisappeared()
        scheduler.runNow()
        assertEquals("账号又在了 ⇒ 不清理", 1, clearedCount)

        accountExists = false
        confirmer.onAccountDisappeared()
        scheduler.runNow()
        assertEquals("账号真的没了 ⇒ 再清理一次", 2, clearedCount)
    }
}

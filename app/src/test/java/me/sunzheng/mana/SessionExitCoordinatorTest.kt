package me.sunzheng.mana

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * design D16：「会话被清掉 ⇒ 离开当前页面回登录页」的触发闸门。
 *
 * 协调器本身要 Android 运行时（Activity/Application），因此这里只锁最容易写错的一处纯逻辑：
 * `observeForever` 的**初始投递**（LiveData 初值 0）不能被当成「刚被清掉」，否则一安装就弹出登录页。
 */
class SessionExitCoordinatorTest {

    @Test
    fun `initial delivery of the live data is not a relocation trigger`() {
        assertFalse(shouldRelocate(previous = -1L, current = 0L))
    }

    @Test
    fun `a bumped revision triggers relocation`() {
        assertTrue(shouldRelocate(previous = 0L, current = 1L))
        assertTrue(shouldRelocate(previous = 4L, current = 5L))
    }

    @Test
    fun `a repeated value does not trigger relocation twice`() {
        assertFalse(shouldRelocate(previous = 1L, current = 1L))
    }

    @Test
    fun `a lower value never triggers relocation`() {
        assertFalse(shouldRelocate(previous = 3L, current = 2L))
    }
}

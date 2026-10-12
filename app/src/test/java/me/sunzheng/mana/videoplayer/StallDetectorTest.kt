package me.sunzheng.mana.videoplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [StallTracker] 的纯 JVM 用例（需求文档 §13.3）：时间由测试注入，不涉及协程与 Android API。
 */
class StallDetectorTest {

    private val threshold = 8_000L

    private fun tracker(startup: Long = threshold, steady: Long = threshold) =
        StallTracker(startupThresholdMs = startup, steadyThresholdMs = steady)

    @Test
    fun `sustained stall at threshold fires once`() {
        val tracker = tracker()
        tracker.onBufferingStart(0L)

        assertNull(tracker.poll(7_999L))
        assertEquals(8_000L, tracker.poll(8_000L))
        // 已触发过，等待下一次 onBufferingStart 才会再触发
        assertNull(tracker.poll(9_000L))
    }

    @Test
    fun `recovering before threshold does not fire`() {
        val tracker = tracker()
        tracker.onBufferingStart(0L)
        tracker.onBufferingEnd(7_900L)

        assertNull(tracker.poll(20_000L))
    }

    @Test
    fun `paused time is not counted`() {
        val tracker = tracker()
        tracker.onBufferingStart(0L)
        // 播放了 1s 后用户暂停
        tracker.onPaused(1_000L)
        // 暂停期间无论等多久都不触发
        assertNull(tracker.poll(30_000L))
        // 恢复后只累计播放中的时间
        tracker.onResumed(30_000L)
        assertNull(tracker.poll(36_999L))
        assertEquals(8_000L, tracker.poll(37_000L))
    }

    @Test
    fun `ready cancels the timer`() {
        val tracker = tracker()
        tracker.onBufferingStart(0L)
        tracker.onReady()

        assertNull(tracker.poll(60_000L))
    }

    @Test
    fun `threshold is startup before first frame and steady afterwards`() {
        val tracker = tracker(
            startup = StallTracker.DEFAULT_STARTUP_THRESHOLD_MS,
            steady = StallTracker.DEFAULT_STEADY_THRESHOLD_MS
        )

        // 首帧之前的起播宽限：60s
        tracker.onBufferingStart(0L)
        assertNull(tracker.poll(59_999L))
        assertEquals(60_000L, tracker.poll(60_000L))

        // 首帧渲染后回到稳态阈值：15s
        tracker.onReady()
        tracker.onBufferingStart(100_000L)
        assertNull(tracker.poll(114_999L))
        assertEquals(15_000L, tracker.poll(115_000L))
    }

    @Test
    fun `confirmed thresholds are 60s startup and 15s steady`() {
        assertEquals(60_000L, StallTracker.DEFAULT_STARTUP_THRESHOLD_MS)
        assertEquals(15_000L, StallTracker.DEFAULT_STEADY_THRESHOLD_MS)
    }

    @Test
    fun `reset restores startup threshold`() {
        val tracker = tracker(
            startup = StallTracker.DEFAULT_STARTUP_THRESHOLD_MS,
            steady = StallTracker.DEFAULT_STEADY_THRESHOLD_MS
        )
        tracker.onReady()
        tracker.reset()

        tracker.onBufferingStart(0L)
        assertNotNull(tracker.poll(60_000L))
    }
}

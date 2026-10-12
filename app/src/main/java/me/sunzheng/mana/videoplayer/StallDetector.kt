package me.sunzheng.mana.videoplayer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 长加载判定（FR-6）。
 *
 * 拆成两层，便于纯 JVM 单测：
 * - [StallTracker]：纯状态机，时间由调用方注入，不含协程与 Android API；
 * - [StallDetector]：协程计时壳，按 [tickMs] 轮询 [StallTracker]。
 *
 * 阈值（与需求方确认的口径）：**首个画面渲染前（首次 STATE_READY 之前）60s，之后 15s**。
 */
class StallTracker(
    /** 起播宽限：首次 STATE_READY（首个画面）之前的阈值。 */
    private val startupThresholdMs: Long = DEFAULT_STARTUP_THRESHOLD_MS,
    /** 稳态阈值：已经开始播放之后的阈值。 */
    private val steadyThresholdMs: Long = DEFAULT_STEADY_THRESHOLD_MS
) {

    private var startedPlayback = false
    private var buffering = false
    private var paused = false
    private var fired = false
    private var startedAt = 0L
    private var accumulatedMs = 0L

    val thresholdMs: Long get() = if (startedPlayback) steadyThresholdMs else startupThresholdMs

    /** 进入缓冲（重复调用只算第一次）。 */
    fun onBufferingStart(nowMs: Long) {
        if (buffering) return
        buffering = true
        fired = false
        accumulatedMs = 0L
        startedAt = nowMs
    }

    /** 缓冲结束。 */
    fun onBufferingEnd(nowMs: Long) {
        if (!buffering) return
        accumulatedMs += (nowMs - startedAt).coerceAtLeast(0L)
        buffering = false
    }

    /** STATE_READY：取消计时，并标记"首个画面已渲染"（此后用稳态阈值）。 */
    fun onReady() {
        buffering = false
        paused = false
        fired = false
        accumulatedMs = 0L
        startedPlayback = true
    }

    /** 用户暂停：暂停期间不计时（FR-6）。 */
    fun onPaused(nowMs: Long) {
        if (paused) return
        freeze(nowMs)
        paused = true
    }

    /** 用户恢复播放。 */
    fun onResumed(nowMs: Long) {
        if (!paused) return
        paused = false
        startedAt = nowMs
    }

    /**
     * 轮询：达到阈值且尚未触发时返回已缓冲时长，其余情况返回 null。
     * 触发后需等下一次 [onBufferingStart] 才会再次触发。
     */
    fun poll(nowMs: Long): Long? {
        if (!buffering || paused || fired) return null
        val elapsed = accumulatedMs + (nowMs - startedAt).coerceAtLeast(0L)
        if (elapsed < thresholdMs) return null
        fired = true
        return elapsed
    }

    fun reset() {
        startedPlayback = false
        buffering = false
        paused = false
        fired = false
        accumulatedMs = 0L
        startedAt = 0L
    }

    private fun freeze(nowMs: Long) {
        if (buffering) {
            accumulatedMs += (nowMs - startedAt).coerceAtLeast(0L)
            startedAt = nowMs
        }
    }

    companion object {
        /** 与需求方确认：首个画面渲染前 60s。 */
        const val DEFAULT_STARTUP_THRESHOLD_MS = 60_000L

        /** 与需求方确认：首个画面之后 15s。 */
        const val DEFAULT_STEADY_THRESHOLD_MS = 15_000L
    }
}

/**
 * 协程计时壳：把播放器回调喂给 [StallTracker]，达到阈值时回调 [onSustainedStall]。
 * 调用方必须在 `onDestroyView` 调用 [cancel]。
 */
class StallDetector(
    private val scope: CoroutineScope,
    private val tracker: StallTracker = StallTracker(),
    private val tickMs: Long = DEFAULT_TICK_MS,
    private val now: () -> Long = { android.os.SystemClock.elapsedRealtime() },
    private val onSustainedStall: (Long) -> Unit
) {

    private var job: Job? = null

    fun onBufferingStart() {
        tracker.onBufferingStart(now())
        ensureLoop()
    }

    fun onBufferingEnd() {
        tracker.onBufferingEnd(now())
    }

    fun onReady() {
        tracker.onReady()
    }

    fun onPaused() {
        tracker.onPaused(now())
    }

    fun onResumed() {
        tracker.onResumed(now())
    }

    fun cancel() {
        job?.cancel()
        job = null
    }

    private fun ensureLoop() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                delay(tickMs)
                val elapsed = tracker.poll(now())
                if (elapsed != null) {
                    onSustainedStall(elapsed)
                    return@launch
                }
            }
        }
    }

    companion object {
        const val DEFAULT_TICK_MS = 500L
    }
}

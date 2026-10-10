package me.sunzheng.mana.utils

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure JVM tests for the ImageKit url builder. [HostUtil.imagekitSupport] deliberately avoids any
 * Android framework api so it can be verified here.
 */
class HostUtilTest {

    private val cover = "https://ik.imagekit.io/acct/path/cover.jpg"

    @Test
    fun `non imagekit url is returned unchanged`() {
        assertEquals(
            "https://cdn.example.com/a.jpg",
            HostUtil.imagekitSupport("https://cdn.example.com/a.jpg", 288f, 384f, 1.333f)
        )
    }

    @Test
    fun `both dimensions are rounded to multiples of twenty`() {
        assertEquals("$cover?tr=w-280,h-380", HostUtil.imagekitSupport(cover, 288f, 384f, 1.333f))
    }

    @Test
    fun `existing query string is extended with ampersand`() {
        assertEquals(
            "$cover?updatedAt=123&tr=w-280,h-380",
            HostUtil.imagekitSupport("$cover?updatedAt=123", 288f, 384f, 1.333f)
        )
    }

    @Test
    fun `trailing question mark does not produce a double separator`() {
        assertEquals("$cover?tr=w-280,h-380", HostUtil.imagekitSupport("$cover?", 288f, 384f, 1.333f))
    }

    @Test
    fun `trailing ampersand does not produce a double separator`() {
        assertEquals(
            "$cover?a=1&tr=w-280,h-380",
            HostUtil.imagekitSupport("$cover?a=1&", 288f, 384f, 1.333f)
        )
    }

    @Test
    fun `height is derived from width and ratio`() {
        assertEquals("$cover?tr=w-300,h-220", HostUtil.imagekitSupport(cover, 300f, -1f, 0.75f))
    }

    @Test
    fun `width is derived from height and ratio`() {
        assertEquals("$cover?tr=h-300,w-400", HostUtil.imagekitSupport(cover, -1f, 300f, 0.75f))
    }

    @Test
    fun `width only when ratio is unknown`() {
        assertEquals("$cover?tr=w-300", HostUtil.imagekitSupport(cover, 300f, -1f, -1f))
    }

    @Test
    fun `height only when ratio is unknown`() {
        assertEquals("$cover?tr=h-300", HostUtil.imagekitSupport(cover, -1f, 300f, -1f))
    }

    @Test
    fun `size original sentinel leaves the url untouched`() {
        val sizeOriginal = Int.MIN_VALUE.toFloat()
        assertEquals(cover, HostUtil.imagekitSupport(cover, sizeOriginal, sizeOriginal, -1f))
    }

    @Test
    fun `unusable tiny dimension leaves the url untouched`() {
        assertEquals(cover, HostUtil.imagekitSupport(cover, 5f, -1f, -1f))
    }

    @Test
    fun `existing explicit transformation wins`() {
        val transformed = "$cover?tr=w-100"
        assertEquals(transformed, HostUtil.imagekitSupport(transformed, 288f, 384f, 1.333f))
    }

    @Test
    fun `existing explicit transformation is detected after another query param`() {
        val transformed = "$cover?q-80&tr=w-100"
        assertEquals(transformed, HostUtil.imagekitSupport(transformed, 288f, 384f, 1.333f))
    }

    @Test
    fun `blank url is returned unchanged`() {
        assertEquals("", HostUtil.imagekitSupport("", 288f, 384f, 1.333f))
    }
}

package com.mid.varagh.core.designsystem.reading

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingColorMatrixTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()
    private val sepiaBg = 0xFFF4ECD8.toInt()
    private val sepiaText = 0xFF3B2F20.toInt()

    @Test
    fun `identity leaves colours unchanged`() {
        assertEquals(0xFF123456.toInt(), ReadingColorMatrix.apply(ReadingColorMatrix.IDENTITY, 0xFF123456.toInt()))
    }

    @Test
    fun `per-channel duotone maps paper to background and ink to text`() {
        val m = ReadingColorMatrix.duotone(sepiaBg, sepiaText, useLuminance = false)
        assertEquals(sepiaBg, ReadingColorMatrix.apply(m, white))
        assertEquals(sepiaText, ReadingColorMatrix.apply(m, black))
    }

    @Test
    fun `luminance duotone maps paper to background and ink to text on dark themes`() {
        val bg = 0xFF000000.toInt()
        val text = 0xFFA88C66.toInt()
        val m = ReadingColorMatrix.duotone(bg, text, useLuminance = true)
        assertEquals(bg, ReadingColorMatrix.apply(m, white))
        assertEquals(text, ReadingColorMatrix.apply(m, black))
    }

    @Test
    fun `dark theme inverts brightness ordering`() {
        val m = ReadingColorMatrix.duotone(0xFF1F2023.toInt(), 0xFFD6D6D0.toInt(), useLuminance = true)
        val lightGrey = ReadingColorMatrix.apply(m, 0xFFCCCCCC.toInt())
        val darkGrey = ReadingColorMatrix.apply(m, 0xFF333333.toInt())
        assertTrue(ReadingColorMatrix.luminance(lightGrey) < ReadingColorMatrix.luminance(darkGrey))
    }

    @Test
    fun `alpha row is preserved`() {
        val m = ReadingColorMatrix.duotone(sepiaBg, sepiaText, useLuminance = false)
        assertArrayEquals(floatArrayOf(0f, 0f, 0f, 1f, 0f), m.copyOfRange(15, 20), 0f)
    }

    @Test
    fun `warm filter is transparent when off and bounded when on`() {
        assertEquals(0f, WarmFilter.overlayColor(0f).alpha)
        assertEquals(WarmFilter.MAX_ALPHA, WarmFilter.overlayColor(5f).alpha, 0.001f)
    }
}

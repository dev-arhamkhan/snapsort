package com.arham.ssorganizer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.arham.ssorganizer.ui.viewer.clampOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ViewerGestureTest {

    // --- Gesture Disambiguation Logic Tests ---

    private fun isUpwardSwipe(dx: Float, dy: Float, touchSlop: Float): Boolean {
        return dy < -touchSlop && abs(dy) > abs(dx) * 1.3f
    }

    private fun isHorizontalSwipe(dx: Float, dy: Float, touchSlop: Float): Boolean {
        return abs(dx) > touchSlop && abs(dx) > abs(dy)
    }

    @Test
    fun testDominantUpwardSwipe() {
        val touchSlop = 16f
        // Vertical upward swipe with minor horizontal drift
        val dx = 6f
        val dy = -45f

        assertTrue("Should detect dominant upward swipe", isUpwardSwipe(dx, dy, touchSlop))
        assertFalse("Should not be classified as horizontal swipe", isHorizontalSwipe(dx, dy, touchSlop))
    }

    @Test
    fun testDominantHorizontalSwipe() {
        val touchSlop = 16f
        // Clear horizontal swipe left
        val dx = -60f
        val dy = -8f

        assertFalse("Should not be classified as upward swipe", isUpwardSwipe(dx, dy, touchSlop))
        assertTrue("Should detect horizontal swipe for pager", isHorizontalSwipe(dx, dy, touchSlop))
    }

    @Test
    fun testDiagonalSwipeDoesNotTriggerAccidentalShare() {
        val touchSlop = 16f
        // Diagonal swipe (both dx and dy similar magnitude)
        val dx = -35f
        val dy = -38f

        assertFalse("Diagonal swipe must not trigger upward share to prevent accidental shares", isUpwardSwipe(dx, dy, touchSlop))
    }

    @Test
    fun testSubThresholdMovementDoesNotTrigger() {
        val touchSlop = 16f
        // Jitter / micro movement below touch slop
        val dx = 4f
        val dy = -10f

        assertFalse("Micro movement below touch slop must not trigger", isUpwardSwipe(dx, dy, touchSlop))
        assertFalse("Micro movement below touch slop must not trigger", isHorizontalSwipe(dx, dy, touchSlop))
    }

    @Test
    fun testDownwardSwipeDoesNotTriggerShare() {
        val touchSlop = 16f
        val dx = 2f
        val dy = 80f // downward drag

        assertFalse("Downward drag must never trigger share", isUpwardSwipe(dx, dy, touchSlop))
    }

    @Test
    fun testShareThresholdTrigger() {
        val thresholdPx = 220f // ~110dp on xxhdpi (density 2.0)
        val smallSwipe = -150f
        val fullSwipe = -250f

        assertFalse("Swipe below threshold must not trigger share", abs(smallSwipe) >= thresholdPx)
        assertTrue("Swipe past threshold must trigger share", abs(fullSwipe) >= thresholdPx)
    }

    // --- Pan Clamp Offset Tests ---

    @Test
    fun testClampAtNormalScale() {
        val size = IntSize(1080, 2340)
        val offset = Offset(100f, 200f)

        // At 1x scale, clamp must always return Offset.Zero
        val clamped = clampOffset(offset, scale = 1.0f, size = size)
        assertEquals(Offset.Zero, clamped)
    }

    @Test
    fun testClampAtZoomedScaleWithinBounds() {
        val size = IntSize(1080, 2340)
        val scale = 2.0f
        // Max pan X = 1080 * 1.0 / 2 = 540
        // Max pan Y = 2340 * 1.0 / 2 = 1170
        val offset = Offset(300f, -500f)

        val clamped = clampOffset(offset, scale = scale, size = size)
        assertEquals(300f, clamped.x, 0.01f)
        assertEquals(-500f, clamped.y, 0.01f)
    }

    @Test
    fun testClampAtZoomedScaleOutOfBounds() {
        val size = IntSize(1080, 2340)
        val scale = 2.0f
        val offset = Offset(900f, -1500f) // Beyond 540 and 1170

        val clamped = clampOffset(offset, scale = scale, size = size)
        assertEquals(540f, clamped.x, 0.01f)
        assertEquals(-1170f, clamped.y, 0.01f)
    }
}

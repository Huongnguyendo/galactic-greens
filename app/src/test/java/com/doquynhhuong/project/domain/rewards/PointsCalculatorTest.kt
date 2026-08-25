package com.doquynhhuong.project.domain.rewards

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [PointsCalculator].
 * Rule under test: 1-5 photos = 10 pts, 6-10 photos = 20 pts,
 * capped at 20 pts per order, 0 or fewer photos = 0 pts.
 */
class PointsCalculatorTest {

    @Test
    fun `zero or negative photos earn zero points`() {
        assertEquals(0, PointsCalculator.pointsForPhotoCount(0))
        assertEquals(0, PointsCalculator.pointsForPhotoCount(-3))
    }

    @Test
    fun `one to five photos earn ten points`() {
        assertEquals(10, PointsCalculator.pointsForPhotoCount(1))
        assertEquals(10, PointsCalculator.pointsForPhotoCount(3))
        assertEquals(10, PointsCalculator.pointsForPhotoCount(5))
    }

    @Test
    fun `six or more photos earn twenty points`() {
        assertEquals(20, PointsCalculator.pointsForPhotoCount(6))
        assertEquals(20, PointsCalculator.pointsForPhotoCount(10))
    }

    @Test
    fun `points never exceed the max per order even with many extra photos`() {
        val points = PointsCalculator.pointsForPhotoCount(500)
        assertEquals(PointsCalculator.MAX_POINTS_PER_ORDER, points)
        assertEquals(20, PointsCalculator.MAX_POINTS_PER_ORDER)
    }

    @Test
    fun `boundary just below the six photo threshold still gives ten points`() {
        // Guards against an off-by-one error in the "6+" branch condition
        assertEquals(10, PointsCalculator.pointsForPhotoCount(5))
        assertEquals(20, PointsCalculator.pointsForPhotoCount(6))
    }
}

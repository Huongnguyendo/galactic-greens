package com.doquynhhuong.project.domain.rewards

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [BadgeTier], which maps a user's total accumulated points
 * to an achievement tier: Explorer (0-100), Curator (101-250), Archivist (251+).
 */
class BadgeTierTest {

    @Test
    fun `zero points is explorer`() {
        assertEquals(BadgeTier.EXPLORER, BadgeTier.forPoints(0))
    }

    @Test
    fun `points just under curator threshold stay explorer`() {
        assertEquals(BadgeTier.EXPLORER, BadgeTier.forPoints(100))
    }

    @Test
    fun `points at curator threshold become curator`() {
        assertEquals(BadgeTier.CURATOR, BadgeTier.forPoints(101))
    }

    @Test
    fun `points at archivist threshold become archivist`() {
        assertEquals(BadgeTier.ARCHIVIST, BadgeTier.forPoints(251))
        assertEquals(BadgeTier.ARCHIVIST, BadgeTier.forPoints(10_000))
    }

    @Test
    fun `fromName round trips a stored enum name and falls back to explorer for bad data`() {
        assertEquals(BadgeTier.CURATOR, BadgeTier.fromName("CURATOR"))
        assertEquals(BadgeTier.EXPLORER, BadgeTier.fromName(null))
        assertEquals(BadgeTier.EXPLORER, BadgeTier.fromName("NOT_A_REAL_TIER"))
    }
}

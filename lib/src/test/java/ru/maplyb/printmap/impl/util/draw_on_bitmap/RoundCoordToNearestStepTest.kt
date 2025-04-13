package ru.maplyb.printmap.impl.util.draw_on_bitmap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoundCoordToNearestStepTest {

    @Test
    fun `test compute value round to lower`() {
        val computeValue = roundCoordToNearestStep(55.1235432)
        val expectedValue = 55.12
        assertEquals(expectedValue, computeValue)
    }

    @Test
    fun `test compute value round to biggest`() {
        val computeValue = roundCoordToNearestStep(55.1265432)
        val expectedValue = 55.13
        assertEquals(expectedValue, computeValue)
    }
}
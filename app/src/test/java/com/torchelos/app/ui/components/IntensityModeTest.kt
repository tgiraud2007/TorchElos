package com.torchelos.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class IntensityModeTest {

    @Test
    fun `levels up to two are nightlight`() {
        assertEquals(IntensityMode.NIGHTLIGHT, IntensityMode.of(1))
        assertEquals(IntensityMode.NIGHTLIGHT, IntensityMode.of(2))
    }

    @Test
    fun `levels up to seventy five are eco`() {
        assertEquals(IntensityMode.ECO, IntensityMode.of(3))
        assertEquals(IntensityMode.ECO, IntensityMode.of(75))
    }

    @Test
    fun `levels up to one hundred seventy five are standard`() {
        assertEquals(IntensityMode.STANDARD, IntensityMode.of(76))
        assertEquals(IntensityMode.STANDARD, IntensityMode.of(175))
    }

    @Test
    fun `levels up to three hundred fifty are bright`() {
        assertEquals(IntensityMode.BRIGHT, IntensityMode.of(176))
        assertEquals(IntensityMode.BRIGHT, IntensityMode.of(350))
    }

    @Test
    fun `levels above three hundred fifty are turbo`() {
        assertEquals(IntensityMode.TURBO, IntensityMode.of(351))
        assertEquals(IntensityMode.TURBO, IntensityMode.of(500))
    }
}

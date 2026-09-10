package com.torchelos.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PocoSysfsTorchEngineTest {

    @Test
    fun `level one is mapped to the hardware minimum`() {
        assertEquals(
            PocoSysfsTorchEngine.HARDWARE_MIN_LEVEL,
            PocoSysfsTorchEngine.mapToHardwareLevel(1)
        )
    }

    @Test
    fun `hardware minimum level is kept as is`() {
        assertEquals(
            PocoSysfsTorchEngine.HARDWARE_MIN_LEVEL,
            PocoSysfsTorchEngine.mapToHardwareLevel(2)
        )
    }

    @Test
    fun `levels above the hardware minimum are not altered`() {
        assertEquals(3, PocoSysfsTorchEngine.mapToHardwareLevel(3))
        assertEquals(130, PocoSysfsTorchEngine.mapToHardwareLevel(130))
        assertEquals(500, PocoSysfsTorchEngine.mapToHardwareLevel(500))
    }

    @Test
    fun `levels outside the range are clamped`() {
        assertEquals(PocoSysfsTorchEngine.HARDWARE_MIN_LEVEL, PocoSysfsTorchEngine.mapToHardwareLevel(0))
        assertEquals(PocoSysfsTorchEngine.HARDWARE_MIN_LEVEL, PocoSysfsTorchEngine.mapToHardwareLevel(-10))
        assertEquals(PocoSysfsTorchEngine.MAX_LEVEL, PocoSysfsTorchEngine.mapToHardwareLevel(501))
        assertEquals(PocoSysfsTorchEngine.MAX_LEVEL, PocoSysfsTorchEngine.mapToHardwareLevel(9999))
    }
}

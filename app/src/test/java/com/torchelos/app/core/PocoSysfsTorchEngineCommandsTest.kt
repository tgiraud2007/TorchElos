package com.torchelos.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PocoSysfsTorchEngineCommandsTest {

    private val forbiddenOperators = listOf("&&", "||", "|", ";", "2>")

    @Test
    fun `disarm commands do not use shell operators`() {
        assertNoShellOperators(PocoSysfsTorchEngine.disarmCommands())
    }

    @Test
    fun `turn on commands do not use shell operators`() {
        assertNoShellOperators(PocoSysfsTorchEngine.turnOnCommands(130))
    }

    @Test
    fun `restore trigger commands do not use shell operators`() {
        assertNoShellOperators(PocoSysfsTorchEngine.restoreTriggersCommands())
    }

    @Test
    fun `switch commands use expected values`() {
        assertEquals(
            "echo 0 > /sys/class/leds/led:switch_0/brightness",
            PocoSysfsTorchEngine.switchCommand(false)
        )
        assertEquals(
            "echo 1 > /sys/class/leds/led:switch_0/brightness",
            PocoSysfsTorchEngine.switchCommand(true)
        )
    }

    @Test
    fun `turn on commands map level one to hardware minimum`() {
        assertEquals(
            listOf(
                "echo 2 > /sys/class/leds/led:torch_0/brightness",
                "echo 1 > /sys/class/leds/led:switch_0/brightness"
            ),
            PocoSysfsTorchEngine.turnOnCommands(1)
        )
    }

    private fun assertNoShellOperators(commands: List<String>) {
        commands.forEach { command ->
            forbiddenOperators.forEach { operator ->
                assertFalse(
                    "Command '$command' must not contain '$operator'",
                    command.contains(operator)
                )
            }
        }
    }
}

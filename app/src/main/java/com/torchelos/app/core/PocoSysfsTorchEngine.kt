package com.torchelos.app.core

import android.util.Log

class PocoSysfsTorchEngine : TorchEngine {

    companion object {
        private const val TAG = "PocoSysfsTorchEngine"

        const val MAX_LEVEL = 500
        const val MIN_LEVEL = 1
        const val HARDWARE_MIN_LEVEL = 2
        const val DEFAULT_LEVEL = 130

        private const val NODE_SWITCH_0 = "/sys/class/leds/led:switch_0/brightness"
        private const val NODE_SWITCH_0_TRIGGER = "/sys/class/leds/led:switch_0/trigger"
        private const val NODE_TORCH_0 = "/sys/class/leds/led:torch_0/brightness"
        private const val NODE_TORCH_0_TRIGGER = "/sys/class/leds/led:torch_0/trigger"
        private const val NODE_TORCH_3_TRIGGER = "/sys/class/leds/led:torch_3/trigger"

        private const val TRIGGER_SWITCH_0 = "switch0_trigger"
        private const val TRIGGER_TORCH_0 = "torch0_trigger"
        private const val TRIGGER_TORCH_3 = "torch3_trigger"

        fun mapToHardwareLevel(level: Int): Int =
            level.coerceIn(MIN_LEVEL, MAX_LEVEL).coerceAtLeast(HARDWARE_MIN_LEVEL)

        internal fun disarmCommands(): List<String> = listOf(
            "echo none > $NODE_SWITCH_0_TRIGGER",
            "echo none > $NODE_TORCH_0_TRIGGER",
            "echo none > $NODE_TORCH_3_TRIGGER"
        )

        internal fun turnOnCommands(level: Int): List<String> = listOf(
            "echo ${mapToHardwareLevel(level)} > $NODE_TORCH_0",
            "echo 1 > $NODE_SWITCH_0"
        )

        internal fun switchCommand(enabled: Boolean): String =
            "echo ${if (enabled) 1 else 0} > $NODE_SWITCH_0"

        internal fun restoreTriggersCommands(): List<String> = listOf(
            "echo $TRIGGER_SWITCH_0 > $NODE_SWITCH_0_TRIGGER",
            "echo $TRIGGER_TORCH_0 > $NODE_TORCH_0_TRIGGER",
            "echo $TRIGGER_TORCH_3 > $NODE_TORCH_3_TRIGGER"
        )
    }

    override fun getMaxLevel(): Int = MAX_LEVEL

    override fun getMinLevel(): Int = MIN_LEVEL

    override fun getDefaultLevel(): Int = DEFAULT_LEVEL

    fun isTorchNodePresent(): Boolean =
        ShellUtils.execSu("test -e $NODE_TORCH_0").isSuccess

    fun disarmTriggers(): Boolean =
        ShellUtils.execSuAll(*disarmCommands().toTypedArray())

    override fun turnOn(level: Int): Boolean {
        val success = ShellUtils.execSuAll(*turnOnCommands(level).toTypedArray())
        if (!success) {
            Log.e(TAG, "Failed to turn on torch")
        }
        return success
    }

    override fun setStrength(level: Int): Boolean =
        ShellUtils.execSu("echo ${mapToHardwareLevel(level)} > $NODE_TORCH_0").isSuccess

    override fun turnOff(): Boolean {
        val switchedOff = setSwitchEnabled(false)
        restoreTriggers()
        return switchedOff
    }

    fun setSwitchEnabled(enabled: Boolean): Boolean =
        ShellUtils.execSu(switchCommand(enabled)).isSuccess

    fun restoreTriggers(): Boolean =
        ShellUtils.execSuAll(*restoreTriggersCommands().toTypedArray())

    fun ensureTriggersRestored() {
        restoreTriggerIfNeeded(NODE_SWITCH_0_TRIGGER, TRIGGER_SWITCH_0)
        restoreTriggerIfNeeded(NODE_TORCH_0_TRIGGER, TRIGGER_TORCH_0)
        restoreTriggerIfNeeded(NODE_TORCH_3_TRIGGER, TRIGGER_TORCH_3)
    }

    private fun restoreTriggerIfNeeded(node: String, trigger: String) {
        val current = ShellUtils.execSu("cat $node")
        if (current.isSuccess && !current.output.contains("[$trigger]")) {
            Log.w(TAG, "Restoring missing trigger $trigger")
            ShellUtils.execSu("echo $trigger > $node")
        }
    }
}

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
    }

    override fun isAvailable(): Boolean =
        ShellUtils.isRootAvailable() && isTorchNodePresent()

    fun isTorchNodePresent(): Boolean =
        ShellUtils.execSu("test -e $NODE_TORCH_0").isSuccess

    override fun getMaxLevel(): Int = MAX_LEVEL

    override fun getMinLevel(): Int = MIN_LEVEL

    override fun getDefaultLevel(): Int = DEFAULT_LEVEL

    fun disarmTriggers(): Boolean {
        val command = "echo none > $NODE_SWITCH_0_TRIGGER && " +
            "echo none > $NODE_TORCH_0_TRIGGER && " +
            "echo none > $NODE_TORCH_3_TRIGGER"
        return ShellUtils.execSu(command).isSuccess
    }

    override fun turnOn(level: Int): Boolean {
        val hwLevel = mapToHardwareLevel(level)
        val command = "echo $hwLevel > $NODE_TORCH_0 && echo 1 > $NODE_SWITCH_0"
        val result = ShellUtils.execSu(command)
        if (!result.isSuccess) {
            Log.e(TAG, "turnOn failed: ${result.output}")
        }
        return result.isSuccess
    }

    override fun setStrength(level: Int): Boolean {
        val hwLevel = mapToHardwareLevel(level)
        return ShellUtils.execSu("echo $hwLevel > $NODE_TORCH_0").isSuccess
    }

    override fun turnOff(): Boolean {
        val command = "echo 0 > $NODE_SWITCH_0 && " +
            "echo $TRIGGER_SWITCH_0 > $NODE_SWITCH_0_TRIGGER && " +
            "echo $TRIGGER_TORCH_0 > $NODE_TORCH_0_TRIGGER && " +
            "echo $TRIGGER_TORCH_3 > $NODE_TORCH_3_TRIGGER"
        return ShellUtils.execSu(command).isSuccess
    }

    fun ensureTriggersRestored() {
        restoreTriggerIfNeeded(NODE_SWITCH_0_TRIGGER, TRIGGER_SWITCH_0)
        restoreTriggerIfNeeded(NODE_TORCH_0_TRIGGER, TRIGGER_TORCH_0)
        restoreTriggerIfNeeded(NODE_TORCH_3_TRIGGER, TRIGGER_TORCH_3)
    }

    private fun restoreTriggerIfNeeded(node: String, trigger: String) {
        val current = ShellUtils.execSu("cat $node 2>/dev/null")
        if (current.isSuccess && !current.output.contains("[$trigger]")) {
            Log.w(TAG, "Restoring missing trigger $trigger")
            ShellUtils.execSu("echo $trigger > $node")
        }
    }
}

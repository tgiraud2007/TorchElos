package com.torchelos.app.core

import android.util.Log

class PocoSysfsTorchEngine : TorchEngine {

    companion object {
        private const val TAG = "PocoSysfsTorchEngine"

        const val NODE_SWITCH_0 = "/sys/class/leds/led:switch_0/brightness"
        const val NODE_SWITCH_0_TRIGGER = "/sys/class/leds/led:switch_0/trigger"
        const val NODE_TORCH_0 = "/sys/class/leds/led:torch_0/brightness"
        const val NODE_TORCH_0_TRIGGER = "/sys/class/leds/led:torch_0/trigger"
        // Note : led:torch_3/brightness est exclu car son DTS noyau bride à 315 mA maximum.
        // On conserve uniquement son trigger pour le restaurer à l'extinction pour la caméra stock.
        const val NODE_TORCH_3_TRIGGER = "/sys/class/leds/led:torch_3/trigger"

        const val MAX_LEVEL = 500
        const val MIN_LEVEL = 1
        const val HARDWARE_MIN_LEVEL = 2 // Level 1 truncates to 0 mA in QTI driver, level 2 yields 12.5 mA (smooth nightlight)
        const val DEFAULT_LEVEL = 130 // 130 mA matches stock factory LineageOS/CamX total current
    }

    override fun isAvailable(): Boolean {
        if (!ShellUtils.isRootAvailable()) return false
        val check = ShellUtils.execSu("test -e $NODE_TORCH_0 && echo OK")
        return check.isSuccess && check.output.contains("OK")
    }

    override fun getMaxLevel(): Int = MAX_LEVEL
    override fun getMinLevel(): Int = MIN_LEVEL
    override fun getDefaultLevel(): Int = DEFAULT_LEVEL

    /**
     * Temporarily disarms Qualcomm CamX triggers (switch_0, torch_0, torch_3).
     * Allows CameraManager to enable system torch state without CamX firing its 65 mA pulse.
     */
    fun disarmTriggers(): Boolean {
        val cmd = "echo none > $NODE_SWITCH_0_TRIGGER && echo none > $NODE_TORCH_0_TRIGGER && echo none > $NODE_TORCH_3_TRIGGER"
        return ShellUtils.execSu(cmd).isSuccess
    }

    override fun turnOn(level: Int): Boolean {
        val clamped = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        val hwValue = if (clamped < HARDWARE_MIN_LEVEL) HARDWARE_MIN_LEVEL else clamped

        // Exclusively drive torch_0 (1..500) and power via switch_0
        val cmd = "echo $hwValue > $NODE_TORCH_0 && echo 1 > $NODE_SWITCH_0"
        val res = ShellUtils.execSu(cmd)
        if (!res.isSuccess) {
            Log.e(TAG, "Failed to turnOn sysfs: ${res.output}")
            return false
        }
        return true
    }

    override fun setStrength(level: Int): Boolean {
        val clamped = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        val hwValue = if (clamped < HARDWARE_MIN_LEVEL) HARDWARE_MIN_LEVEL else clamped

        // Fast direct write to torch_0 only
        val cmd = "echo $hwValue > $NODE_TORCH_0"
        return ShellUtils.execSu(cmd).isSuccess
    }

    override fun turnOff(): Boolean {
        // Turn off switch_0 and atomically restore all system triggers for LineageOS / Camera
        val cmd = "echo 0 > $NODE_SWITCH_0 && echo switch0_trigger > $NODE_SWITCH_0_TRIGGER && echo torch0_trigger > $NODE_TORCH_0_TRIGGER && echo torch3_trigger > $NODE_TORCH_3_TRIGGER"
        return ShellUtils.execSu(cmd).isSuccess
    }

    fun ensureTriggersRestored() {
        // Restore switch0_trigger, torch0_trigger, and torch3_trigger if missing or [none]
        val resSwitch = ShellUtils.execSu("cat $NODE_SWITCH_0_TRIGGER 2>/dev/null")
        if (resSwitch.isSuccess && !resSwitch.output.contains("[switch0_trigger]")) {
            Log.w(TAG, "switch0_trigger missing, restoring...")
            ShellUtils.execSu("echo switch0_trigger > $NODE_SWITCH_0_TRIGGER")
        }

        val resTorch0 = ShellUtils.execSu("cat /sys/class/leds/led:torch_0/trigger 2>/dev/null")
        if (resTorch0.isSuccess && !resTorch0.output.contains("[torch0_trigger]")) {
            Log.w(TAG, "torch0_trigger missing, restoring...")
            ShellUtils.execSu("echo torch0_trigger > /sys/class/leds/led:torch_0/trigger")
        }

        val resTorch3 = ShellUtils.execSu("cat /sys/class/leds/led:torch_3/trigger 2>/dev/null")
        if (resTorch3.isSuccess && !resTorch3.output.contains("[torch3_trigger]")) {
            Log.w(TAG, "torch3_trigger missing, restoring...")
            ShellUtils.execSu("echo torch3_trigger > /sys/class/leds/led:torch_3/trigger")
        }
    }
}

package com.torchelos.app.core

import android.util.Log

class PocoSysfsTorchEngine : TorchEngine {

    companion object {
        private const val TAG = "PocoSysfsTorchEngine"

        const val NODE_SWITCH_0 = "/sys/class/leds/led:switch_0/brightness"
        const val NODE_SWITCH_0_TRIGGER = "/sys/class/leds/led:switch_0/trigger"
        const val NODE_TORCH_0 = "/sys/class/leds/led:torch_0/brightness"
        const val NODE_TORCH_3 = "/sys/class/leds/led:torch_3/brightness"
        const val NODE_TORCH_0_TRIGGER = "/sys/class/leds/led:torch_0/trigger"
        const val NODE_TORCH_3_TRIGGER = "/sys/class/leds/led:torch_3/trigger"

        const val MAX_LEVEL = 500
        const val MIN_LEVEL = 1
        const val HARDWARE_MIN_LEVEL = 2 // 1 tronque à 0 mA (éteint) dans le driver QTI, 2 donne 12.5 mA (veilleuse douce)
        const val DEFAULT_LEVEL = 65
    }

    override val id: String = "poco_sysfs_pm8350c"
    override val displayName: String = "KernelSU PM8350C Sysfs (Matériel)"

    private var isOnState = false
    private var currentLevelState = DEFAULT_LEVEL

    override fun isAvailable(): Boolean {
        if (!ShellUtils.isRootAvailable()) return false
        val check = ShellUtils.execSu("test -e $NODE_TORCH_0 && echo OK")
        return check.isSuccess && check.output.contains("OK")
    }

    override fun getMaxLevel(): Int = MAX_LEVEL
    override fun getMinLevel(): Int = MIN_LEVEL
    override fun getDefaultLevel(): Int = DEFAULT_LEVEL

    override fun turnOn(level: Int): Boolean {
        val clamped = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        val hwValue = if (clamped < HARDWARE_MIN_LEVEL) HARDWARE_MIN_LEVEL else clamped
        currentLevelState = clamped

        // Allumage direct à la puissance voulue : ZÉRO flash à 65mA, ZÉRO délai !
        val cmd = "echo $hwValue > $NODE_TORCH_0 && echo $hwValue > $NODE_TORCH_3 && echo 1 > $NODE_SWITCH_0"
        val res = ShellUtils.execSu(cmd)
        if (res.isSuccess) {
            isOnState = true
            return true
        } else {
            Log.e(TAG, "Échec turnOn sysfs: ${res.output}")
            return false
        }
    }

    override fun setStrength(level: Int): Boolean {
        val clamped = level.coerceIn(MIN_LEVEL, MAX_LEVEL)
        currentLevelState = clamped
        val hwValue = if (clamped < HARDWARE_MIN_LEVEL) HARDWARE_MIN_LEVEL else clamped

        val cmd = "echo $hwValue > $NODE_TORCH_0 && echo $hwValue > $NODE_TORCH_3"
        val res = ShellUtils.execSu(cmd)
        return res.isSuccess
    }

    override fun turnOff(): Boolean {
        isOnState = false
        // Éteindre immédiatement switch_0 et restaurer atomiquement les triggers pour LineageOS
        val cmd = "echo 0 > $NODE_SWITCH_0 && echo switch0_trigger > $NODE_SWITCH_0_TRIGGER && echo torch0_trigger > $NODE_TORCH_0_TRIGGER && echo torch3_trigger > $NODE_TORCH_3_TRIGGER"
        val res = ShellUtils.execSu(cmd)
        return res.isSuccess
    }

    override fun isTorchOn(): Boolean {
        val res = ShellUtils.execSu("cat $NODE_SWITCH_0 2>/dev/null")
        if (res.isSuccess && res.output.trim() == "1") {
            isOnState = true
            return true
        }
        if (res.isSuccess && res.output.trim() == "0") {
            isOnState = false
            return false
        }
        return isOnState
    }

    fun readHardwareLevel(): Int {
        val res = ShellUtils.execSu("cat $NODE_TORCH_0 2>/dev/null")
        return res.output.trim().toIntOrNull() ?: currentLevelState
    }

    fun ensureTriggersRestored() {
        // Restaurer switch0_trigger, torch0_trigger et torch3_trigger s'ils sont manquants ou [none]
        val resSwitch = ShellUtils.execSu("cat $NODE_SWITCH_0_TRIGGER 2>/dev/null")
        if (resSwitch.isSuccess && !resSwitch.output.contains("[switch0_trigger]")) {
            Log.w(TAG, "switch0_trigger absent, restauration...")
            ShellUtils.execSu("echo switch0_trigger > $NODE_SWITCH_0_TRIGGER")
        }

        val resTorch0 = ShellUtils.execSu("cat /sys/class/leds/led:torch_0/trigger 2>/dev/null")
        if (resTorch0.isSuccess && !resTorch0.output.contains("[torch0_trigger]")) {
            Log.w(TAG, "torch0_trigger absent, restauration...")
            ShellUtils.execSu("echo torch0_trigger > /sys/class/leds/led:torch_0/trigger")
        }

        val resTorch3 = ShellUtils.execSu("cat /sys/class/leds/led:torch_3/trigger 2>/dev/null")
        if (resTorch3.isSuccess && !resTorch3.output.contains("[torch3_trigger]")) {
            Log.w(TAG, "torch3_trigger absent, restauration...")
            ShellUtils.execSu("echo torch3_trigger > /sys/class/leds/led:torch_3/trigger")
        }
    }
}

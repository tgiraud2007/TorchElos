package com.torchelos.app.core

import android.content.Context
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TorchState(
    val isOn: Boolean = false,
    val level: Int = 130,
    val maxLevel: Int = 500,
    val minLevel: Int = 1,
    val isRootAvailable: Boolean = false,
    val rootType: String = "Not detected",
    val deviceName: String = "",
    val flashHardware: String = "",
    val romInfo: String = ""
)

class TorchManager(private val context: Context) {

    companion object {
        private const val TAG = "TorchManager"
        private const val PREFS_NAME = "torchelos_prefs"
        private const val KEY_LAST_LEVEL = "last_level"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val pocoEngine = PocoSysfsTorchEngine()
    private val camera2Engine = Camera2TorchEngine(context)

    private var currentEngine: TorchEngine = pocoEngine

    private val _state = MutableStateFlow(TorchState())
    val state: StateFlow<TorchState> = _state.asStateFlow()

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private var sliderDebounceJob: Job? = null

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId != "0") return
            super.onTorchModeChanged(cameraId, enabled)

            sliderDebounceJob?.cancel()
            _state.value = _state.value.copy(isOn = enabled)

            if (enabled) {
                val savedLevel = prefs.getInt(KEY_LAST_LEVEL, PocoSysfsTorchEngine.DEFAULT_LEVEL)
                _state.value = _state.value.copy(isOn = true, level = savedLevel)
                scope.launch {
                    pocoEngine.setStrength(savedLevel)
                    notifyTileUpdate()
                }
            } else {
                _state.value = _state.value.copy(isOn = false)
                scope.launch {
                    pocoEngine.turnOff()
                    pocoEngine.ensureTriggersRestored()
                    notifyTileUpdate()
                }
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            super.onTorchModeUnavailable(cameraId)
            if (cameraId == "0") {
                sliderDebounceJob?.cancel()
                _state.value = _state.value.copy(isOn = false)
                scope.launch {
                    pocoEngine.turnOff()
                    pocoEngine.ensureTriggersRestored()
                    notifyTileUpdate()
                }
            }
        }
    }

    fun init() {
        scope.launch {
            val rootOk = ShellUtils.isRootAvailable()
            val rootSolution = ShellUtils.detectRootSolution()
            val pocoOk = pocoEngine.isAvailable()

            currentEngine = if (pocoOk) {
                pocoEngine
            } else {
                camera2Engine
            }

            // Restore triggers if missing
            pocoEngine.ensureTriggersRestored()

            val savedLevel = prefs.getInt(KEY_LAST_LEVEL, currentEngine.getDefaultLevel())

            _state.value = TorchState(
                isOn = false,
                level = savedLevel,
                maxLevel = currentEngine.getMaxLevel(),
                minLevel = currentEngine.getMinLevel(),
                isRootAvailable = rootOk,
                rootType = rootSolution,
                deviceName = detectDeviceName(),
                flashHardware = detectFlashHardware(),
                romInfo = detectRomInfo()
            )

            // Register system callback for bidirectional sync with QS tile
            try {
                cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                Log.w(TAG, "registerTorchCallback failed", e)
            }
        }
    }

    fun toggleTorch(): Boolean {
        return if (_state.value.isOn) {
            turnOff()
        } else {
            turnOn(_state.value.level)
        }
    }

    fun turnOn(level: Int = _state.value.level): Boolean {
        val targetLevel = level.coerceIn(currentEngine.getMinLevel(), currentEngine.getMaxLevel())
        saveLevel(targetLevel)
        _state.value = _state.value.copy(isOn = true, level = targetLevel)

        val ok = if (currentEngine is PocoSysfsTorchEngine) {
            // 1. Temporarily disarm Qualcomm CamX triggers to suppress the 65 mA factory pulse
            pocoEngine.disarmTriggers()

            // 2. Enable via CameraManager so LineageOS Quick Settings tile becomes active
            try {
                cameraManager.setTorchMode("0", true)
            } catch (e: Exception) {
                Log.w(TAG, "setTorchMode(true) failed", e)
            }

            // 3. Directly power the LED at desired target level
            pocoEngine.turnOn(targetLevel)
        } else {
            try {
                cameraManager.setTorchMode("0", true)
                true
            } catch (e: Exception) {
                Log.w(TAG, "setTorchMode(true) failed", e)
                false
            }
        }

        refreshState()
        notifyTileUpdate()
        return ok
    }

    fun turnOff(): Boolean {
        sliderDebounceJob?.cancel()
        _state.value = _state.value.copy(isOn = false)

        val ok = if (currentEngine is PocoSysfsTorchEngine) {
            // 1. Physically turn off the LED immediately
            val res = pocoEngine.turnOff()

            // 2. Turn off via CameraManager -> LineageOS QS tile reflects state
            try {
                cameraManager.setTorchMode("0", false)
            } catch (e: Exception) {
                Log.w(TAG, "setTorchMode(false) failed", e)
            }

            // 3. Ensure system triggers are restored for the camera app
            pocoEngine.ensureTriggersRestored()
            res
        } else {
            try {
                cameraManager.setTorchMode("0", false)
                true
            } catch (e: Exception) {
                Log.w(TAG, "setTorchMode(false) failed", e)
                false
            }
        }

        refreshState()
        notifyTileUpdate()
        return ok
    }

    fun setLevel(newLevel: Int) {
        val clamped = newLevel.coerceIn(currentEngine.getMinLevel(), currentEngine.getMaxLevel())
        saveLevel(clamped)
        _state.value = _state.value.copy(level = clamped)

        sliderDebounceJob?.cancel()
        sliderDebounceJob = scope.launch {
            delay(15) // Smooth 60fps debounce
            pocoEngine.setStrength(clamped)
            notifyTileUpdate()
        }
    }

    fun notifyTileUpdate() {
        try {
            android.service.quicksettings.TileService.requestListeningState(
                context,
                android.content.ComponentName(context, com.torchelos.app.service.TorchTileService::class.java)
            )
        } catch (e: Exception) {
            Log.w(TAG, "requestListeningState failed", e)
        }
    }

    fun refreshState() {
        scope.launch {
            val rootOk = ShellUtils.isRootAvailable()
            val rootSolution = ShellUtils.detectRootSolution()
            _state.value = _state.value.copy(
                maxLevel = currentEngine.getMaxLevel(),
                minLevel = currentEngine.getMinLevel(),
                isRootAvailable = rootOk,
                rootType = rootSolution,
                deviceName = detectDeviceName(),
                flashHardware = detectFlashHardware(),
                romInfo = detectRomInfo()
            )
        }
    }

    fun cleanup() {
        if (currentEngine is PocoSysfsTorchEngine) {
            pocoEngine.ensureTriggersRestored()
        }
    }

    private fun detectDeviceName(): String {
        val model = android.os.Build.MODEL.trim()
        val device = android.os.Build.DEVICE.trim()
        return if (model.contains(device, ignoreCase = true)) {
            model
        } else {
            "$model ($device)"
        }
    }

    private fun detectFlashHardware(): String {
        val device = android.os.Build.DEVICE.lowercase()
        return when {
            device == "marble" || device == "marblein" -> "Qualcomm PM8350C"
            pocoEngine.isAvailable() -> "Qualcomm QTI Flash (led:torch_0)"
            else -> "Standard Camera HAL"
        }
    }

    private fun detectRomInfo(): String {
        val release = android.os.Build.VERSION.RELEASE
        val lineageDisplay = getSystemProperty("ro.lineage.display.version")
        val lineageVer = getSystemProperty("ro.lineage.version")
        val crdroidVer = getSystemProperty("ro.crdroid.version")
        val display = android.os.Build.DISPLAY
        return when {
            lineageDisplay.isNotEmpty() -> "LineageOS $lineageDisplay (Android $release)"
            lineageVer.isNotEmpty() -> "LineageOS $lineageVer (Android $release)"
            crdroidVer.isNotEmpty() -> "crDroid $crdroidVer (Android $release)"
            display.contains("lineage", ignoreCase = true) -> "LineageOS (Android $release)"
            else -> "Android $release"
        }
    }

    private fun getSystemProperty(key: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("getprop", key))
            p.inputStream.bufferedReader().use { it.readLine()?.trim() ?: "" }
        } catch (e: Exception) {
            ""
        }
    }

    private fun saveLevel(level: Int) {
        prefs.edit().putInt(KEY_LAST_LEVEL, level).apply()
    }
}

package com.torchelos.app.core

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import android.util.Log
import com.torchelos.app.service.TorchTileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class TorchState(
    val isOn: Boolean = false,
    val level: Int = PocoSysfsTorchEngine.DEFAULT_LEVEL,
    val maxLevel: Int = PocoSysfsTorchEngine.MAX_LEVEL,
    val minLevel: Int = PocoSysfsTorchEngine.MIN_LEVEL,
    val isRootAvailable: Boolean = false,
    val rootType: String = ShellUtils.ROOT_NONE,
    val deviceName: String = "",
    val flashHardware: String = "",
    val romInfo: String = "",
    val isHardwareControlled: Boolean = false
)

class TorchManager(private val context: Context) {

    companion object {
        private const val TAG = "TorchManager"
        private const val PREFS_NAME = "torchelos_prefs"
        private const val KEY_LAST_LEVEL = "last_level"
        private const val CAMERA_ID = "0"
        private const val SLIDER_DEBOUNCE_MS = 15L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val hardwareMutex = Mutex()

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
            if (cameraId != CAMERA_ID) return
            scope.launch {
                hardwareMutex.withLock {
                    if (enabled == _state.value.isOn) return@withLock
                    if (enabled) {
                        val savedLevel = savedLevel()
                        _state.update { it.copy(isOn = true, level = savedLevel) }
                        currentEngine.setStrength(savedLevel)
                    } else {
                        _state.update { it.copy(isOn = false) }
                        currentEngine.turnOff()
                    }
                }
                notifyTileUpdate()
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId != CAMERA_ID) return
            scope.launch {
                hardwareMutex.withLock {
                    _state.update { it.copy(isOn = false) }
                    currentEngine.turnOff()
                }
                notifyTileUpdate()
            }
        }
    }

    fun init() {
        scope.launch {
            detectEnvironment()
            registerTorchCallback()
        }
    }

    fun refreshState() {
        scope.launch {
            if (ShellUtils.isRootAvailable() != _state.value.isRootAvailable) {
                detectEnvironment()
            }
        }
    }

    fun toggleTorch() {
        scope.launch {
            if (_state.value.isOn) turnOff() else turnOn()
        }
    }

    suspend fun turnOn(level: Int = _state.value.level) {
        val target = level.coerceIn(currentEngine.getMinLevel(), currentEngine.getMaxLevel())
        saveLevel(target)
        _state.update { it.copy(isOn = true, level = target) }
        notifyTileUpdate()

        val success = withContext(Dispatchers.IO) {
            hardwareMutex.withLock { applyTurnOn(target) }
        }
        if (!success) {
            if (currentEngine is PocoSysfsTorchEngine) setSystemTorchMode(false)
            _state.update { it.copy(isOn = false) }
            notifyTileUpdate()
        }
    }

    suspend fun turnOff() {
        sliderDebounceJob?.cancel()
        _state.update { it.copy(isOn = false) }
        notifyTileUpdate()

        val success = withContext(Dispatchers.IO) {
            hardwareMutex.withLock { applyTurnOff() }
        }
        if (!success) {
            _state.update { it.copy(isOn = true) }
            notifyTileUpdate()
        }
    }

    fun setLevel(level: Int) {
        val target = level.coerceIn(currentEngine.getMinLevel(), currentEngine.getMaxLevel())
        if (target == _state.value.level) return
        saveLevel(target)
        _state.update { it.copy(level = target) }

        sliderDebounceJob?.cancel()
        sliderDebounceJob = scope.launch {
            delay(SLIDER_DEBOUNCE_MS)
            hardwareMutex.withLock {
                if (_state.value.isOn) currentEngine.setStrength(target)
            }
            notifyTileUpdate()
        }
    }

    fun notifyTileUpdate() {
        try {
            TileService.requestListeningState(
                context,
                ComponentName(context, TorchTileService::class.java)
            )
        } catch (e: Exception) {
            Log.w(TAG, "requestListeningState failed", e)
        }
    }

    private fun applyTurnOn(target: Int): Boolean {
        return if (currentEngine is PocoSysfsTorchEngine) {
            pocoEngine.disarmTriggers()
            setSystemTorchMode(true)
            pocoEngine.turnOn(target)
        } else {
            currentEngine.turnOn(target)
        }
    }

    private fun applyTurnOff(): Boolean {
        return if (currentEngine is PocoSysfsTorchEngine) {
            val turnedOff = pocoEngine.turnOff()
            setSystemTorchMode(false)
            turnedOff
        } else {
            currentEngine.turnOff()
        }
    }

    private fun setSystemTorchMode(enabled: Boolean) {
        try {
            cameraManager.setTorchMode(CAMERA_ID, enabled)
        } catch (e: Exception) {
            Log.w(TAG, "setTorchMode($enabled) failed", e)
        }
    }

    private fun registerTorchCallback() {
        try {
            cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
        } catch (e: Exception) {
            Log.w(TAG, "registerTorchCallback failed", e)
        }
    }

    private fun detectEnvironment() {
        val rootAvailable = ShellUtils.isRootAvailable()
        val pocoAvailable = rootAvailable && pocoEngine.isTorchNodePresent()
        currentEngine = if (pocoAvailable) pocoEngine else camera2Engine
        if (pocoAvailable) pocoEngine.ensureTriggersRestored()
        val rootType = if (rootAvailable) ShellUtils.detectRootSolution() else ShellUtils.ROOT_NONE

        val storedLevel = prefs.getInt(KEY_LAST_LEVEL, currentEngine.getDefaultLevel())
        _state.update { current ->
            val level = if (current.isOn) current.level else storedLevel
            current.copy(
                level = level.coerceIn(currentEngine.getMinLevel(), currentEngine.getMaxLevel()),
                maxLevel = currentEngine.getMaxLevel(),
                minLevel = currentEngine.getMinLevel(),
                isRootAvailable = rootAvailable,
                rootType = rootType,
                deviceName = detectDeviceName(),
                flashHardware = detectFlashHardware(pocoAvailable),
                romInfo = detectRomInfo(),
                isHardwareControlled = pocoAvailable
            )
        }
    }

    private fun savedLevel(): Int =
        prefs.getInt(KEY_LAST_LEVEL, currentEngine.getDefaultLevel())

    private fun saveLevel(level: Int) {
        prefs.edit().putInt(KEY_LAST_LEVEL, level).apply()
    }

    private fun detectDeviceName(): String {
        val model = Build.MODEL.trim()
        val device = Build.DEVICE.trim()
        return if (model.contains(device, ignoreCase = true)) model else "$model ($device)"
    }

    private fun detectFlashHardware(pocoAvailable: Boolean): String = when {
        Build.DEVICE.lowercase() in setOf("marble", "marblein") -> "Qualcomm PM8350C"
        pocoAvailable -> "Qualcomm QTI Flash (led:torch_0)"
        else -> "Standard Camera HAL"
    }

    private fun detectRomInfo(): String {
        val release = Build.VERSION.RELEASE
        val lineageDisplay = systemProperty("ro.lineage.display.version")
        val lineageVersion = systemProperty("ro.lineage.version")
        val crDroidVersion = systemProperty("ro.crdroid.version")
        return when {
            lineageDisplay.isNotEmpty() -> "LineageOS $lineageDisplay (Android $release)"
            lineageVersion.isNotEmpty() -> "LineageOS $lineageVersion (Android $release)"
            crDroidVersion.isNotEmpty() -> "crDroid $crDroidVersion (Android $release)"
            Build.DISPLAY.contains("lineage", ignoreCase = true) -> "LineageOS (Android $release)"
            else -> "Android $release"
        }
    }

    private fun systemProperty(key: String): String = try {
        val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
        process.inputStream.bufferedReader().use { it.readLine()?.trim().orEmpty() }
    } catch (e: Exception) {
        ""
    }
}

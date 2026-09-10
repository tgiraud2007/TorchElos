package com.torchelos.app.core

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.hardware.camera2.CameraCharacteristics
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
import java.util.concurrent.TimeUnit

data class TorchState(
    val isDetecting: Boolean = true,
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
        private const val KEY_MAX_LEVEL = "max_level"
        private const val KEY_MIN_LEVEL = "min_level"
        private const val KEY_HARDWARE_CONTROLLED = "hardware_controlled"
        private const val SLIDER_DEBOUNCE_MS = 15L
        private const val FALLBACK_CAMERA_ID = "0"
        private const val SYSTEM_PROPERTY_TIMEOUT_SECONDS = 5L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val hardwareMutex = Mutex()
    private val toggleMutex = Mutex()
    private val detectionMutex = Mutex()

    private val pocoEngine = PocoSysfsTorchEngine()
    private val camera2Engine = Camera2TorchEngine(context)

    @Volatile
    private var currentEngine: TorchEngine = pocoEngine

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<TorchState> = _state.asStateFlow()

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private val flashCameraId: String by lazy { detectFlashCameraId() }

    private var sliderDebounceJob: Job? = null

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId != flashCameraId) return
            scope.launch {
                hardwareMutex.withLock {
                    if (enabled == _state.value.isOn) return@withLock
                    if (enabled) {
                        val savedLevel = savedLevel()
                        _state.update { it.copy(isOn = true, level = savedLevel) }
                        applyExternalTurnOn(savedLevel)
                    } else {
                        _state.update { it.copy(isOn = false) }
                        applyTurnOff()
                    }
                }
                notifyTileUpdate()
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId != flashCameraId) return
            scope.launch {
                hardwareMutex.withLock {
                    _state.update { it.copy(isOn = false) }
                    applyTurnOff()
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
            toggleMutex.withLock {
                if (_state.value.isOn) turnOff() else turnOn()
            }
        }
    }

    suspend fun turnOn(level: Int = _state.value.level) {
        sliderDebounceJob?.cancel()
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

    fun requestTurnOff() {
        scope.launch { turnOff() }
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
            if (!pocoEngine.disarmTriggers()) {
                Log.w(TAG, "Aborting turn-on: failed to disarm CamX triggers")
                return false
            }
            setSystemTorchMode(true)
            pocoEngine.turnOn(target)
        } else {
            currentEngine.turnOn(target)
        }
    }

    private fun applyExternalTurnOn(level: Int) {
        if (currentEngine is PocoSysfsTorchEngine) {
            pocoEngine.disarmTriggers()
            pocoEngine.turnOn(level)
        } else {
            currentEngine.setStrength(level)
        }
    }

    private fun applyTurnOff(): Boolean {
        return if (currentEngine is PocoSysfsTorchEngine) {
            val switchedOff = pocoEngine.setSwitchEnabled(false)
            setSystemTorchMode(false)
            pocoEngine.restoreTriggers()
            switchedOff
        } else {
            currentEngine.turnOff()
        }
    }

    private fun setSystemTorchMode(enabled: Boolean) {
        try {
            cameraManager.setTorchMode(flashCameraId, enabled)
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

    private suspend fun detectEnvironment() = detectionMutex.withLock {
        val rootAvailable = ShellUtils.isRootAvailable()
        val pocoAvailable = rootAvailable && pocoEngine.isTorchNodePresent()
        currentEngine = if (pocoAvailable) pocoEngine else camera2Engine
        if (pocoAvailable) pocoEngine.ensureTriggersRestored()
        val rootType = if (rootAvailable) ShellUtils.detectRootSolution() else ShellUtils.ROOT_NONE

        prefs.edit()
            .putInt(KEY_MAX_LEVEL, currentEngine.getMaxLevel())
            .putInt(KEY_MIN_LEVEL, currentEngine.getMinLevel())
            .putBoolean(KEY_HARDWARE_CONTROLLED, pocoAvailable)
            .apply()

        val storedLevel = prefs.getInt(KEY_LAST_LEVEL, currentEngine.getDefaultLevel())
        _state.update { current ->
            val level = if (current.isOn) current.level else storedLevel
            current.copy(
                isDetecting = false,
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

    private fun initialState(): TorchState {
        val cachedMaxLevel = prefs.getInt(KEY_MAX_LEVEL, 0)
        val hasCache = cachedMaxLevel > 0
        val maxLevel = if (hasCache) cachedMaxLevel else PocoSysfsTorchEngine.MAX_LEVEL
        val minLevel = if (hasCache) {
            prefs.getInt(KEY_MIN_LEVEL, PocoSysfsTorchEngine.MIN_LEVEL)
        } else {
            PocoSysfsTorchEngine.MIN_LEVEL
        }
        return TorchState(
            isDetecting = !hasCache,
            level = prefs.getInt(KEY_LAST_LEVEL, PocoSysfsTorchEngine.DEFAULT_LEVEL)
                .coerceIn(minLevel, maxLevel),
            maxLevel = maxLevel,
            minLevel = minLevel,
            isHardwareControlled = prefs.getBoolean(KEY_HARDWARE_CONTROLLED, false)
        )
    }

    private fun savedLevel(): Int =
        prefs.getInt(KEY_LAST_LEVEL, currentEngine.getDefaultLevel())

    private fun saveLevel(level: Int) {
        prefs.edit().putInt(KEY_LAST_LEVEL, level).apply()
    }

    private fun detectDeviceName(): String {
        val model = Build.MODEL.trim()
        val device = Build.DEVICE.trim()
        return when {
            model.isEmpty() -> device
            model.contains(device, ignoreCase = true) -> model
            else -> "$model ($device)"
        }
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

    private fun detectFlashCameraId(): String {
        return try {
            cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                    characteristics.get(CameraCharacteristics.LENS_FACING) ==
                    CameraCharacteristics.LENS_FACING_BACK
            } ?: FALLBACK_CAMERA_ID
        } catch (e: Exception) {
            Log.w(TAG, "Flash camera detection failed", e)
            FALLBACK_CAMERA_ID
        }
    }

    private fun systemProperty(key: String): String = try {
        val process = Runtime.getRuntime().exec(arrayOf("getprop", key))
        val value = process.inputStream.bufferedReader().use { it.readLine()?.trim().orEmpty() }
        if (!process.waitFor(SYSTEM_PROPERTY_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
        }
        value
    } catch (e: Exception) {
        ""
    }
}

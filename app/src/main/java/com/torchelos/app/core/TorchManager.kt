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
    val level: Int = 65,
    val maxLevel: Int = 500,
    val minLevel: Int = 1,
    val activeEngineId: String = "",
    val activeEngineName: String = "",
    val isRootAvailable: Boolean = false,
    val hardwareCurrentReadout: String = ""
)

class TorchManager(private val context: Context) {

    companion object {
        private const val TAG = "TorchManager"
        private const val PREFS_NAME = "torchelos_prefs"
        private const val KEY_LAST_LEVEL = "last_level"
        private const val KEY_ENGINE = "selected_engine"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val pocoEngine = PocoSysfsTorchEngine()
    val camera2Engine = Camera2TorchEngine(context)

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
            camera2Engine.updateTorchState(enabled)

            sliderDebounceJob?.cancel()
            _state.value = _state.value.copy(isOn = enabled)

            if (enabled) {
                val savedLevel = prefs.getInt(KEY_LAST_LEVEL, PocoSysfsTorchEngine.DEFAULT_LEVEL)
                _state.value = _state.value.copy(isOn = true, level = savedLevel)
                scope.launch {
                    pocoEngine.setStrength(savedLevel)
                    refreshState()
                    notifyTileUpdate()
                }
            } else {
                _state.value = _state.value.copy(isOn = false)
                scope.launch {
                    pocoEngine.turnOff()
                    pocoEngine.ensureTriggersRestored()
                    refreshState()
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
                    refreshState()
                    notifyTileUpdate()
                }
            }
        }
    }

    fun init() {
        scope.launch {
            val rootOk = ShellUtils.isRootAvailable()
            val pocoOk = pocoEngine.isAvailable()

            currentEngine = if (pocoOk) {
                pocoEngine
            } else {
                camera2Engine
            }

            // Vérifier et restaurer les 3 triggers s'ils sont manquants
            pocoEngine.ensureTriggersRestored()

            val savedLevel = prefs.getInt(KEY_LAST_LEVEL, currentEngine.getDefaultLevel())

            _state.value = TorchState(
                isOn = false,
                level = savedLevel,
                maxLevel = currentEngine.getMaxLevel(),
                minLevel = currentEngine.getMinLevel(),
                activeEngineId = currentEngine.id,
                activeEngineName = currentEngine.displayName,
                isRootAvailable = rootOk
            )

            // Écouter les changements système pour la synchronisation parfaite
            try {
                cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
            } catch (e: Exception) {
                Log.w(TAG, "Erreur registerTorchCallback", e)
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
            // 1. Désarmer temporairement Qualcomm CamX en détachant les triggers
            ShellUtils.execSu("echo none > /sys/class/leds/led:switch_0/trigger && echo none > /sys/class/leds/led:torch_0/trigger && echo none > /sys/class/leds/led:torch_3/trigger")

            // 2. Allumer via CameraManager -> la tuile officielle LineageOS s'allume sans que CamX ne puisse allumer la LED à 65mA
            try {
                cameraManager.setTorchMode("0", true)
            } catch (e: Exception) {
                Log.w(TAG, "setTorchMode(true) échoué", e)
            }

            // 3. Allumer directement le matériel à l'intensité souhaitée (zéro flash, instantané)
            pocoEngine.turnOn(targetLevel)
        } else {
            try {
                cameraManager.setTorchMode("0", true)
                true
            } catch (e: Exception) {
                Log.w(TAG, "setTorchMode(true) échoué", e)
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
            // 1. Éteindre physiquement la LED immédiatement
            val res = pocoEngine.turnOff()

            // 2. Éteindre via CameraManager -> la tuile officielle LineageOS s'éteint
            try {
                cameraManager.setTorchMode("0", false)
            } catch (e: Exception) {
                Log.w(TAG, "setTorchMode(false) échoué", e)
            }

            // 3. S'assurer que les triggers système sont restaurés pour la caméra
            pocoEngine.ensureTriggersRestored()
            res
        } else {
            try {
                cameraManager.setTorchMode("0", false)
                true
            } catch (e: Exception) {
                Log.w(TAG, "setTorchMode(false) échoué", e)
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

        // Que la lampe soit allumée ou éteinte, on met à jour les registres matériels :
        // - Si allumée : l'intensité varie instantanément en direct
        // - Si éteinte : les registres sont prêts pour démarrer directement à cette intensité
        sliderDebounceJob?.cancel()
        sliderDebounceJob = scope.launch {
            delay(15) // Débouncing fluide à ~60fps
            pocoEngine.setStrength(clamped)
            val readout = pocoEngine.readHardwareLevel().toString()
            _state.value = _state.value.copy(hardwareCurrentReadout = readout)
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
            Log.w(TAG, "requestListeningState échoué", e)
        }
    }

    fun refreshState() {
        scope.launch {
            val readout = if (currentEngine is PocoSysfsTorchEngine) {
                pocoEngine.readHardwareLevel().toString()
            } else ""

            _state.value = _state.value.copy(
                maxLevel = currentEngine.getMaxLevel(),
                minLevel = currentEngine.getMinLevel(),
                activeEngineId = currentEngine.id,
                activeEngineName = currentEngine.displayName,
                hardwareCurrentReadout = readout
            )
        }
    }

    private fun saveLevel(level: Int) {
        prefs.edit().putInt(KEY_LAST_LEVEL, level).apply()
    }
}

package com.torchelos.app.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.torchelos.app.R
import com.torchelos.app.TorchApp
import com.torchelos.app.core.PocoSysfsTorchEngine
import com.torchelos.app.core.TorchState
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TorchTileService : TileService() {

    private val torchManager by lazy { TorchApp.instance.torchManager }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var stateJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTile(torchManager.state.value)

        stateJob?.cancel()
        stateJob = scope.launch {
            torchManager.state.collectLatest { state -> updateTile(state) }
        }
    }

    override fun onStopListening() {
        stateJob?.cancel()
        super.onStopListening()
    }

    override fun onDestroy() {
        stateJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        torchManager.toggleTorch()
    }

    private fun updateTile(state: TorchState) {
        val tile = qsTile ?: return
        tile.state = if (state.isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_name)

        val percentage = if (state.maxLevel > 0) {
            (state.level * 100f / state.maxLevel).roundToInt()
        } else {
            0
        }
        tile.subtitle = when {
            !state.isOn -> getString(R.string.tile_level_off, state.level, state.maxLevel)
            state.isHardwareControlled && state.level <= PocoSysfsTorchEngine.HARDWARE_MIN_LEVEL ->
                getString(R.string.tile_nightlight, state.level, state.maxLevel)
            state.level >= state.maxLevel ->
                getString(R.string.tile_max, state.level, state.maxLevel)
            else -> getString(
                R.string.tile_level_on,
                state.level,
                state.maxLevel,
                percentage
            )
        }
        tile.updateTile()
    }
}

package com.torchelos.app.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.torchelos.app.R
import com.torchelos.app.TorchApp
import com.torchelos.app.core.PocoSysfsTorchEngine
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
        val current = torchManager.state.value
        updateTile(current.isOn, current.level, current.maxLevel)

        stateJob?.cancel()
        stateJob = scope.launch {
            torchManager.state.collectLatest { state ->
                updateTile(state.isOn, state.level, state.maxLevel)
            }
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

    private fun updateTile(isOn: Boolean, level: Int, maxLevel: Int) {
        val tile = qsTile ?: return
        tile.state = if (isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_name)
        tile.subtitle = when {
            !isOn -> getString(R.string.tile_level_off, level, maxLevel)
            level <= PocoSysfsTorchEngine.HARDWARE_MIN_LEVEL ->
                getString(R.string.tile_nightlight, level, maxLevel)
            level >= maxLevel -> getString(R.string.tile_max, level, maxLevel)
            else -> getString(
                R.string.tile_level_on,
                level,
                maxLevel,
                level * 100 / maxLevel
            )
        }
        tile.updateTile()
    }
}

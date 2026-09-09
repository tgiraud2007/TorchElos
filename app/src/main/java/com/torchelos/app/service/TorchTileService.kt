package com.torchelos.app.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.torchelos.app.MainActivity
import com.torchelos.app.TorchApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

    override fun onClick() {
        super.onClick()
        torchManager.toggleTorch()
    }

    private fun updateTile(isOn: Boolean, level: Int, maxLevel: Int) {
        val tile = qsTile ?: return

        tile.state = if (isOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Torch"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pct = (level * 100) / maxLevel
            val desc = when {
                level <= 2 -> "Nightlight ($level/500)"
                level >= maxLevel -> "Max (500/500)"
                else -> "$level / $maxLevel ($pct%)"
            }
            tile.subtitle = if (isOn) desc else "$level / $maxLevel"
        }

        tile.updateTile()
    }
}

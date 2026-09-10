package com.torchelos.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torchelos.app.R
import com.torchelos.app.ui.theme.DarkSurface
import com.torchelos.app.ui.theme.OnDarkTextPrimary
import com.torchelos.app.ui.theme.TorchAmber

private data class TorchPreset(
    @StringRes val labelRes: Int,
    val level: Int
)

private val PRESETS = listOf(
    TorchPreset(R.string.intensity_nightlight, 1),
    TorchPreset(R.string.intensity_eco, 50),
    TorchPreset(R.string.intensity_standard, 130),
    TorchPreset(R.string.intensity_turbo, 500)
)

@Composable
fun PresetChips(
    currentLevel: Int,
    minLevel: Int,
    maxLevel: Int,
    onSelectLevel: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val presets = remember(minLevel, maxLevel) {
        PRESETS
            .map { preset ->
                TorchPreset(preset.labelRes, preset.level.coerceIn(minLevel, maxLevel))
            }
            .distinctBy { it.level }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        presets.forEach { preset ->
            val isSelected = currentLevel == preset.level

            Surface(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onSelectLevel(preset.level)
                },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) TorchAmber else DarkSurface,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) TorchAmber else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = stringResource(preset.labelRes),
                        color = if (isSelected) Color.Black else OnDarkTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

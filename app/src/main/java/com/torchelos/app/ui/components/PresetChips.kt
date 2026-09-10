package com.torchelos.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torchelos.app.ui.theme.*

data class TorchPreset(
    val label: String,
    val level: Int
)

@Composable
fun PresetChips(
    currentLevel: Int,
    maxLevel: Int,
    onSelectLevel: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val presets = listOf(
        TorchPreset("Nightlight", 1),
        TorchPreset("Eco", 50),
        TorchPreset("Standard", 130),
        TorchPreset("Turbo", 500)
    )

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
                    onSelectLevel(preset.level.coerceIn(1, maxLevel))
                },
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) TorchAmber else DarkSurface,
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
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
                        text = preset.label,
                        color = if (isSelected) Color.Black else OnDarkTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

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

data class Preset(
    val label: String,
    val percentage: Int
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
        Preset("10%", 10),
        Preset("25%", 25),
        Preset("50%", 50),
        Preset("75%", 75),
        Preset("Max", 100)
    )

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        presets.forEach { preset ->
            val targetLevel = ((preset.percentage / 100f) * maxLevel).toInt().coerceAtLeast(1)
            val isSelected = (currentLevel == targetLevel) ||
                    (preset.percentage == 100 && currentLevel == maxLevel)

            Surface(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onSelectLevel(targetLevel)
                },
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) TorchAmber else DarkSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 3.dp)
                    .height(44.dp)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) TorchAmber else Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = preset.label,
                        color = if (isSelected) Color.Black else OnDarkTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

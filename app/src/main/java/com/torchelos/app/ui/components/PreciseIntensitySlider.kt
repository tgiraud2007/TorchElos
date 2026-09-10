package com.torchelos.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torchelos.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreciseIntensitySlider(
    currentLevel: Int,
    minLevel: Int,
    maxLevel: Int,
    onLevelChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var showEditDialog by remember { mutableStateOf(false) }

    val percentage = (currentLevel * 100f / maxLevel).toInt()

    val modeName = when {
        currentLevel <= 2 -> "Nightlight"
        currentLevel <= 75 -> "Eco"
        currentLevel <= 175 -> "Standard"
        currentLevel <= 350 -> "Bright"
        else -> "Turbo"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(22.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header: Mode info on left, clickable level badge on right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Brightness",
                        color = OnDarkTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = modeName,
                        color = OnDarkTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Clickable badge for direct numeric entry
                Surface(
                    onClick = { showEditDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "$currentLevel",
                            color = TorchAmber,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " / $maxLevel",
                            color = OnDarkTextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit value",
                            tint = OnDarkTextSecondary,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Smooth high-definition Slider
            Slider(
                value = currentLevel.toFloat(),
                onValueChange = { newValue ->
                    val intVal = newValue.toInt().coerceIn(minLevel, maxLevel)
                    if (intVal != currentLevel) {
                        if (intVal % 25 == 0 || intVal == minLevel || intVal == maxLevel) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        onLevelChanged(intVal)
                    }
                },
                valueRange = minLevel.toFloat()..maxLevel.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = TorchAmber,
                    activeTrackColor = TorchAmber,
                    inactiveTrackColor = DarkSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Slider bound markers
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "1 (Min)",
                    color = OnDarkTextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Text(
                    text = "$percentage%",
                    color = TorchAmber.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "500 (Turbo)",
                    color = OnDarkTextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }

    // Direct numeric input dialog
    if (showEditDialog) {
        var textInput by remember { mutableStateOf(currentLevel.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text("Exact Intensity", color = OnDarkTextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Enter value between $minLevel and $maxLevel:",
                        color = OnDarkTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it.filter { ch -> ch.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TorchAmber,
                            focusedTextColor = OnDarkTextPrimary,
                            unfocusedTextColor = OnDarkTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = textInput.toIntOrNull()
                        if (parsed != null) {
                            onLevelChanged(parsed.coerceIn(minLevel, maxLevel))
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("Apply", color = TorchAmber, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = OnDarkTextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

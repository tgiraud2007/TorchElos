package com.torchelos.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Entête avec affichage de la valeur exacte cliquable pour saisie directe
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Puissance du flash",
                    color = OnDarkTextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                // Badge de valeur cliquable
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .clickable {
                            showEditDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$currentLevel",
                        color = TorchAmber,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " / $maxLevel ($percentage%)",
                        color = OnDarkTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Éditer la valeur",
                        tint = OnDarkTextSecondary,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Slider continu 1 à 500
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

            Spacer(modifier = Modifier.height(12.dp))

            // Boutons de réglage fin pas-à-pas (+1, -1, +10, -10)
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                StepButton(text = "-10") {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onLevelChanged((currentLevel - 10).coerceIn(minLevel, maxLevel))
                }
                StepButton(text = "-1") {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onLevelChanged((currentLevel - 1).coerceIn(minLevel, maxLevel))
                }
                StepButton(text = "+1") {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onLevelChanged((currentLevel + 1).coerceIn(minLevel, maxLevel))
                }
                StepButton(text = "+10") {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    onLevelChanged((currentLevel + 10).coerceIn(minLevel, maxLevel))
                }
            }
        }
    }

    // Dialogue pour taper directement une valeur au clavier (ex: 62, 356)
    if (showEditDialog) {
        var textInput by remember { mutableStateOf(currentLevel.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text("Entrer une valeur exacte", color = OnDarkTextPrimary)
            },
            text = {
                Column {
                    Text(
                        "Saisissez une valeur entre $minLevel et $maxLevel :",
                        color = OnDarkTextSecondary,
                        fontSize = 14.sp
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
                    Text("Appliquer", color = TorchAmber, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Annuler", color = OnDarkTextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun StepButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = DarkSurfaceVariant,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 14.dp)
        ) {
            Text(
                text = text,
                color = OnDarkTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

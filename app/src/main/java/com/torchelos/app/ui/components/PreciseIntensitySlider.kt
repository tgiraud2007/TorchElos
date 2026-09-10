package com.torchelos.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torchelos.app.R
import com.torchelos.app.ui.theme.DarkSurface
import com.torchelos.app.ui.theme.DarkSurfaceVariant
import com.torchelos.app.ui.theme.OnDarkTextPrimary
import com.torchelos.app.ui.theme.OnDarkTextSecondary
import com.torchelos.app.ui.theme.TorchAmber

enum class IntensityMode(@StringRes val labelRes: Int) {
    NIGHTLIGHT(R.string.intensity_nightlight),
    ECO(R.string.intensity_eco),
    STANDARD(R.string.intensity_standard),
    BRIGHT(R.string.intensity_bright),
    TURBO(R.string.intensity_turbo);

    companion object {
        fun of(level: Int): IntensityMode = when {
            level <= 2 -> NIGHTLIGHT
            level <= 75 -> ECO
            level <= 175 -> STANDARD
            level <= 350 -> BRIGHT
            else -> TURBO
        }
    }
}

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

    val percentage = if (maxLevel > 0) currentLevel * 100 / maxLevel else 0
    val mode = IntensityMode.of(currentLevel)

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.brightness),
                        color = OnDarkTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(mode.labelRes),
                        color = OnDarkTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

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
                            text = currentLevel.toString(),
                            color = TorchAmber,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.level_fraction_suffix, maxLevel),
                            color = OnDarkTextSecondary,
                            fontSize = 12.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_value),
                            tint = OnDarkTextSecondary,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Slider(
                value = currentLevel.toFloat(),
                onValueChange = { newValue ->
                    val intValue = newValue.toInt().coerceIn(minLevel, maxLevel)
                    if (intValue != currentLevel) {
                        if (intValue % 25 == 0 || intValue == minLevel || intValue == maxLevel) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        }
                        onLevelChanged(intValue)
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

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = stringResource(R.string.min_level_label, minLevel),
                    color = OnDarkTextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
                Text(
                    text = stringResource(R.string.percentage, percentage),
                    color = TorchAmber.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.max_level_label, maxLevel),
                    color = OnDarkTextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }

    if (showEditDialog) {
        var textInput by remember { mutableStateOf(currentLevel.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.exact_intensity),
                    color = OnDarkTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.enter_value_between, minLevel, maxLevel),
                        color = OnDarkTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it.filter(Char::isDigit) },
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
                        textInput.toIntOrNull()?.let { value ->
                            onLevelChanged(value.coerceIn(minLevel, maxLevel))
                        }
                        showEditDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.apply),
                        color = TorchAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        color = OnDarkTextSecondary
                    )
                }
            },
            containerColor = DarkSurface
        )
    }
}

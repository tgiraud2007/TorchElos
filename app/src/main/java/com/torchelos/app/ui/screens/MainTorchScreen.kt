package com.torchelos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torchelos.app.core.TorchState
import com.torchelos.app.ui.components.PowerButton
import com.torchelos.app.ui.components.PreciseIntensitySlider
import com.torchelos.app.ui.components.PresetChips
import com.torchelos.app.ui.theme.*

@Composable
fun MainTorchScreen(
    state: TorchState,
    onToggle: () -> Unit,
    onLevelChanged: (Int) -> Unit,
    onOpenDiagnostic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val intensityRatio = state.level.toFloat() / state.maxLevel.toFloat()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Application Title with Flash icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = TorchAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TorchElos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnDarkTextPrimary
                    )
                }

                // Settings button
                IconButton(
                    onClick = onOpenDiagnostic,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = OnDarkTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(bottom = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Central Power Button & Live Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    PowerButton(
                        isOn = state.isOn,
                        intensityRatio = intensityRatio,
                        onToggle = onToggle
                    )
                }

                Text(
                    text = if (state.isOn) "ON • ${state.level} mA" else "TAP TO TURN ON",
                    color = if (state.isOn) TorchAmber else OnDarkTextSecondary.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = if (state.isOn) 0.5.sp else 1.2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Quick Preset Modes
            PresetChips(
                currentLevel = state.level,
                maxLevel = state.maxLevel,
                onSelectLevel = onLevelChanged,
                modifier = Modifier.padding(bottom = 14.dp)
            )

            // Precision Intensity Slider
            PreciseIntensitySlider(
                currentLevel = state.level,
                minLevel = state.minLevel,
                maxLevel = state.maxLevel,
                onLevelChanged = onLevelChanged
            )
        }
    }
}

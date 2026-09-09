package com.torchelos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Titre de l'application
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

                // Bouton Diagnostic / Test Matériel
                IconButton(
                    onClick = onOpenDiagnostic,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Build,
                        contentDescription = "Hardware Diagnostics",
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
                .padding(bottom = 24.dp)
        ) {
            // Badge d'état du moteur actif
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (state.isRootAvailable) GreenSuccess else RedInactive)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state.isRootAvailable) "POCO F5 (PM8350C) • Root Active" else "Standard Camera Mode",
                    color = OnDarkTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Bouton tactile central ON/OFF
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

            // Puces de raccourcis rapides
            PresetChips(
                currentLevel = state.level,
                maxLevel = state.maxLevel,
                onSelectLevel = onLevelChanged,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Slider continu 1 à 500 avec boutons pas-à-pas et saisie directe
            PreciseIntensitySlider(
                currentLevel = state.level,
                minLevel = state.minLevel,
                maxLevel = state.maxLevel,
                onLevelChanged = onLevelChanged
            )
        }
    }
}

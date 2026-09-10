package com.torchelos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torchelos.app.R
import com.torchelos.app.core.TorchState
import com.torchelos.app.ui.components.PowerButton
import com.torchelos.app.ui.components.PreciseIntensitySlider
import com.torchelos.app.ui.components.PresetChips
import com.torchelos.app.ui.theme.DarkBackground
import com.torchelos.app.ui.theme.DarkSurface
import com.torchelos.app.ui.theme.DarkSurfaceVariant
import com.torchelos.app.ui.theme.OnDarkTextPrimary
import com.torchelos.app.ui.theme.OnDarkTextSecondary
import com.torchelos.app.ui.theme.TorchAmber

@Composable
fun MainTorchScreen(
    state: TorchState,
    onToggle: () -> Unit,
    onLevelChanged: (Int) -> Unit,
    onOpenDiagnostic: () -> Unit,
    modifier: Modifier = Modifier
) {
    val intensityRatio = if (state.maxLevel > 0) {
        state.level.toFloat() / state.maxLevel
    } else {
        0f
    }

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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = TorchAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnDarkTextPrimary
                    )
                }

                IconButton(
                    onClick = onOpenDiagnostic,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = OnDarkTextPrimary,
                        modifier = Modifier.size(22.dp)
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
                    text = when {
                        state.isOn && state.isHardwareControlled ->
                            stringResource(R.string.status_on_ma, state.level)
                        state.isOn -> stringResource(R.string.status_on)
                        else -> stringResource(R.string.status_tap_to_turn_on)
                    },
                    color = if (state.isOn) TorchAmber else OnDarkTextSecondary.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = if (state.isOn) 0.5.sp else 1.2.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (state.maxLevel > 1) {
                PresetChips(
                    currentLevel = state.level,
                    maxLevel = state.maxLevel,
                    onSelectLevel = onLevelChanged,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                PreciseIntensitySlider(
                    currentLevel = state.level,
                    minLevel = state.minLevel,
                    maxLevel = state.maxLevel,
                    onLevelChanged = onLevelChanged
                )
            } else {
                IntensityUnavailableCard(modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

@Composable
private fun IntensityUnavailableCard(modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.intensity_unavailable_title),
                color = TorchAmber,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.intensity_unavailable_message),
                color = OnDarkTextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

package com.torchelos.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.torchelos.app.ui.theme.DarkSurface
import com.torchelos.app.ui.theme.DarkSurfaceVariant
import com.torchelos.app.ui.theme.TorchAmber
import com.torchelos.app.ui.theme.TorchAmberDark
import com.torchelos.app.ui.theme.TorchGlow

@Composable
fun PowerButton(
    isOn: Boolean,
    intensityRatio: Float,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }

    val glowRadius = animateDpAsState(
        targetValue = if (isOn) (24.dp + (32.dp * intensityRatio)) else 0.dp,
        animationSpec = tween(300),
        label = "glowRadius"
    )

    val buttonScale = animateFloatAsState(
        targetValue = if (isOn) 1.03f else 1.0f,
        animationSpec = tween(200),
        label = "buttonScale"
    )

    val iconColor = animateColorAsState(
        targetValue = if (isOn) Color.Black else Color.White.copy(alpha = 0.6f),
        animationSpec = tween(200),
        label = "iconColor"
    )

    val backgroundBrush = if (isOn) {
        Brush.radialGradient(
            colors = listOf(
                TorchAmber,
                TorchAmberDark
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                DarkSurfaceVariant,
                DarkSurface
            )
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(190.dp)
            .scale(buttonScale.value)
    ) {
        // Lueur extérieure réactive si allumé
        if (isOn) {
            Box(
                modifier = Modifier
                    .size(160.dp + glowRadius.value)
                    .clip(CircleShape)
                    .background(TorchGlow.copy(alpha = 0.2f + (intensityRatio * 0.4f)))
            )
        }

        // Bouton principal
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(140.dp)
                .shadow(
                    elevation = if (isOn) 16.dp else 4.dp,
                    shape = CircleShape,
                    spotColor = if (isOn) TorchAmber else Color.Black
                )
                .clip(CircleShape)
                .background(backgroundBrush)
                .border(
                    width = if (isOn) 3.dp else 2.dp,
                    color = if (isOn) TorchAmber.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.15f),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    onToggle()
                }
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = if (isOn) "Turn off flashlight" else "Turn on flashlight",
                tint = iconColor.value,
                modifier = Modifier.size(64.dp)
            )
        }
    }
}

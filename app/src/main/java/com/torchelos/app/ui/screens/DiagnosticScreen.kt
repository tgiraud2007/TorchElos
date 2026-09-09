package com.torchelos.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torchelos.app.core.TorchManager
import com.torchelos.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    torchManager: TorchManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by torchManager.state.collectAsState()
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isRunningAutoTest by remember { mutableStateOf(false) }
    var autoTestStatus by remember { mutableStateOf("") }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hardware Diagnostics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnDarkTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnDarkTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // Carte État Système & Root
            InfoCard(title = "System Environment") {
                InfoRow("Device", "Xiaomi POCO F5 (marble)")
                InfoRow("OS", "LineageOS 23.2 (Android 16)")
                InfoRow(
                    "Root Access",
                    if (state.isRootAvailable) "Operational (uid=0)" else "Not detected",
                    isSuccess = state.isRootAvailable
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Carte Camera HAL vs Matériel
            InfoCard(title = "Hardware Analysis") {
                InfoRow("Camera2 HAL", "strengthMaximumLevel = 1 (No ROM slider)", isWarning = true)
                InfoRow("PMIC Controller", "Qualcomm PM8350C (leds-qti-flash)")
                InfoRow("Torch Nodes", "led:torch_0 (500) + led:torch_3 (500)")
                InfoRow("Switch Node", "led:switch_0 (Channel mask 0x09)")
                InfoRow("Available Range", "1 to 500 continuous steps")
                InfoRow("Stock LineageOS Level", "65 (13% max power)")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section Banc de Test Physique
            Text(
                text = "Hardware Power Testbench",
                color = OnDarkTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Visually verify that the physical flash LED changes intensity:",
                color = OnDarkTextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Bouton Test automatique séquentiel
            Button(
                onClick = {
                    if (!isRunningAutoTest) {
                        coroutineScope.launch {
                            isRunningAutoTest = true
                            val testSteps = listOf(
                                25 to "Level 25 (5% - Dim nightlight)",
                                75 to "Level 75 (15% - Eco)",
                                150 to "Level 150 (30% - Moderate)",
                                300 to "Level 300 (60% - Bright)",
                                500 to "Level 500 (100% - Full Turbo!)"
                            )

                            for ((level, desc) in testSteps) {
                                autoTestStatus = desc
                                torchManager.turnOn(level)
                                delay(2000)
                            }

                            autoTestStatus = "Test finished - Powering off"
                            torchManager.turnOff()
                            delay(1000)
                            autoTestStatus = ""
                            isRunningAutoTest = false
                        }
                    }
                },
                enabled = !isRunningAutoTest,
                colors = ButtonDefaults.buttonColors(containerColor = TorchAmber),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunningAutoTest) "Testing in progress..." else "Run sequential brightness test",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }

            if (autoTestStatus.isNotEmpty()) {
                Text(
                    text = autoTestStatus,
                    color = TorchAmber,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Paliers de test individuels
            Text(
                text = "Instant manual steps:",
                color = OnDarkTextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(25, 65, 150, 300, 500).forEach { lvl ->
                    OutlinedButton(
                        onClick = { torchManager.turnOn(lvl) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TorchAmber),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TorchAmber.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$lvl",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bouton Éteindre
            Button(
                onClick = { torchManager.turnOff() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Turn off flashlight", color = OnDarkTextPrimary)
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = TorchAmber,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    isSuccess: Boolean? = null,
    isWarning: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = OnDarkTextSecondary,
            fontSize = 13.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = when {
                    isSuccess == true -> GreenSuccess
                    isWarning -> TorchAmber
                    else -> OnDarkTextPrimary
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (isSuccess == true) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = GreenSuccess,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(14.dp)
                )
            } else if (isWarning) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = TorchAmber,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(14.dp)
                )
            }
        }
    }
}

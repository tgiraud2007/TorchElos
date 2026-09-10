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
    var isRunningTest by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler {
        onBack()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Hardware",
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Hardware & System Status Card
            Text(
                text = "System Status",
                color = OnDarkTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusRow(
                        label = "Root Access",
                        value = state.rootType.ifEmpty { if (state.isRootAvailable) "Operational" else "Not detected" },
                        isSuccess = state.isRootAvailable
                    )
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    StatusRow(
                        label = "Device",
                        value = state.deviceName.ifEmpty { "POCO F5 (marble)" },
                        isSuccess = true
                    )
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.05f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                    StatusRow(
                        label = "Flash Driver",
                        value = state.flashHardware.ifEmpty { "Qualcomm PM8350C" },
                        isSuccess = state.flashHardware != "Standard Camera HAL"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Hardware Test Section
            Text(
                text = "Hardware Test",
                color = OnDarkTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Brightness Ramp Test",
                        color = OnDarkTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Smoothly ramps the flash from Nightlight (1) up to 200 mA over 2 seconds to verify the physical LED without blinding your eyes.",
                        color = OnDarkTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )

                    Button(
                        onClick = {
                            if (!isRunningTest) {
                                coroutineScope.launch {
                                    isRunningTest = true
                                    val steps = listOf(1, 25, 75, 130, 200)
                                    for (lvl in steps) {
                                        torchManager.turnOn(lvl)
                                        delay(400)
                                    }
                                    delay(400)
                                    torchManager.turnOff()
                                    isRunningTest = false
                                }
                            }
                        },
                        enabled = !isRunningTest,
                        colors = ButtonDefaults.buttonColors(containerColor = TorchAmber),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunningTest) "Testing flash..." else "Run Brightness Test",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Thermal Safety Advisory
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, TorchAmber.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = TorchAmber,
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Thermal Notice",
                            color = TorchAmber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Turbo level (500) drives 500 mA of continuous current. For daily illumination, levels between 1 and 130 (Standard) provide ideal brightness while staying cool and efficient.",
                            color = OnDarkTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "TorchElos v1.1.0-beta",
                    color = OnDarkTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${state.deviceName.ifEmpty { "POCO F5 (marble)" }} • ${state.romInfo.ifEmpty { "Android" }}",
                    color = OnDarkTextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    isSuccess: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = OnDarkTextSecondary,
            fontSize = 13.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                color = if (isSuccess) OnDarkTextPrimary else TorchAmber,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isSuccess) GreenSuccess else TorchAmber,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(15.dp)
            )
        }
    }
}

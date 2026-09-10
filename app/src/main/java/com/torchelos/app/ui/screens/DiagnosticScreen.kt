package com.torchelos.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.torchelos.app.BuildConfig
import com.torchelos.app.R
import com.torchelos.app.core.TorchManager
import com.torchelos.app.ui.theme.DarkBackground
import com.torchelos.app.ui.theme.DarkSurface
import com.torchelos.app.ui.theme.GreenSuccess
import com.torchelos.app.ui.theme.OnDarkTextPrimary
import com.torchelos.app.ui.theme.OnDarkTextSecondary
import com.torchelos.app.ui.theme.TorchAmber
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RAMP_TEST_LEVELS = listOf(1, 25, 75, 130, 200)

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

    DisposableEffect(Unit) {
        onDispose {
            if (isRunningTest) {
                torchManager.requestTurnOff()
            }
        }
    }

    BackHandler { onBack() }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_hardware_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnDarkTextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = OnDarkTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
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
            SectionTitle(stringResource(R.string.system_status))

            StatusCard {
                StatusRow(
                    label = stringResource(R.string.root_access),
                    value = if (state.isRootAvailable) {
                        stringResource(R.string.root_operational, state.rootType)
                    } else {
                        stringResource(R.string.not_detected)
                    },
                    isSuccess = state.isRootAvailable
                )
                RowDivider()
                StatusRow(
                    label = stringResource(R.string.device),
                    value = state.deviceName
                )
                RowDivider()
                StatusRow(
                    label = stringResource(R.string.flash_driver),
                    value = state.flashHardware,
                    isSuccess = state.isHardwareControlled
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionTitle(stringResource(R.string.hardware_test))

            StatusCard {
                Text(
                    text = stringResource(R.string.ramp_test_title),
                    color = OnDarkTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.ramp_test_description),
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
                                for (level in RAMP_TEST_LEVELS) {
                                    torchManager.turnOn(level)
                                    delay(400)
                                }
                                torchManager.turnOff()
                                isRunningTest = false
                            }
                        }
                    },
                    enabled = !isRunningTest && state.maxLevel > 1,
                    colors = ButtonDefaults.buttonColors(containerColor = TorchAmber),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (isRunningTest) R.string.ramp_test_running else R.string.run_ramp_test
                        ),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.thermal_notice),
                            color = TorchAmber,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.thermal_notice_text),
                            color = OnDarkTextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                    color = OnDarkTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(
                        R.string.device_rom_format,
                        state.deviceName,
                        state.romInfo
                    ),
                    color = OnDarkTextSecondary.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = OnDarkTextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun StatusCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.padding(vertical = 10.dp)
    )
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    isSuccess: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = OnDarkTextSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.weight(1f))
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

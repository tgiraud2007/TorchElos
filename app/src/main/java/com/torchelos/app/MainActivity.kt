package com.torchelos.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.torchelos.app.ui.screens.DiagnosticScreen
import com.torchelos.app.ui.screens.MainTorchScreen
import com.torchelos.app.ui.theme.DarkBackground
import com.torchelos.app.ui.theme.TorchElosTheme

class MainActivity : ComponentActivity() {

    private val torchManager by lazy { TorchApp.instance.torchManager }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                torchManager.refreshState()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Demander la permission Camera si nécessaire
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            TorchElosTheme {
                val state by torchManager.state.collectAsState()
                var isDiagnosticOpen by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    AnimatedContent(
                        targetState = isDiagnosticOpen,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ScreenTransition"
                    ) { showDiag ->
                        if (showDiag) {
                            DiagnosticScreen(
                                torchManager = torchManager,
                                onBack = { isDiagnosticOpen = false }
                            )
                        } else {
                            MainTorchScreen(
                                state = state,
                                onToggle = { torchManager.toggleTorch() },
                                onLevelChanged = { newLvl -> torchManager.setLevel(newLvl) },
                                onOpenDiagnostic = { isDiagnosticOpen = true }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        torchManager.refreshState()
    }
}

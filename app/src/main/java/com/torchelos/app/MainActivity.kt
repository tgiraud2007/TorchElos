package com.torchelos.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.torchelos.app.ui.screens.DiagnosticScreen
import com.torchelos.app.ui.screens.MainTorchScreen
import com.torchelos.app.ui.theme.DarkBackground
import com.torchelos.app.ui.theme.TorchElosTheme

class MainActivity : ComponentActivity() {

    private val torchManager by lazy { TorchApp.instance.torchManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TorchElosTheme {
                TorchElosApp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        torchManager.refreshState()
    }

    @Composable
    private fun TorchElosApp() {
        val state by torchManager.state.collectAsState()
        var isDiagnosticOpen by remember { mutableStateOf(false) }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            BackHandler(enabled = isDiagnosticOpen) {
                isDiagnosticOpen = false
            }

            AnimatedContent(
                targetState = isDiagnosticOpen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScreenTransition"
            ) { showDiagnostic ->
                if (showDiagnostic) {
                    DiagnosticScreen(
                        torchManager = torchManager,
                        onBack = { isDiagnosticOpen = false }
                    )
                } else {
                    MainTorchScreen(
                        state = state,
                        onToggle = torchManager::toggleTorch,
                        onLevelChanged = torchManager::setLevel,
                        onOpenDiagnostic = { isDiagnosticOpen = true }
                    )
                }
            }
        }
    }
}

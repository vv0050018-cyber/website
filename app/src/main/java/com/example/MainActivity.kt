package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.KidneyBottomBar
import com.example.ui.components.KidneyTopAppBar
import com.example.ui.screens.*
import com.example.ui.theme.KidneyAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KidneyAiTheme {
                KidneyAiApp()
            }
        }
    }
}

@Composable
fun KidneyAiApp(viewModel: MainViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()

    if (currentUser == null) {
        AuthScreen(viewModel = viewModel)
    } else {
        Scaffold(
            topBar = {
                KidneyTopAppBar(
                    currentScreen = currentScreen,
                    userName = currentUser?.name,
                    onNavigateBack = if (currentScreen !is Screen.Dashboard) {
                        { viewModel.navigateTo(Screen.Dashboard) }
                    } else null,
                    onLogoutClick = { viewModel.logout() }
                )
            },
            bottomBar = {
                KidneyBottomBar(
                    currentScreen = currentScreen,
                    onTabSelected = { screen -> viewModel.navigateTo(screen) }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val screen = currentScreen) {
                    is Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
                    is Screen.Analyze -> AnalyzeScreen(viewModel = viewModel)
                    is Screen.Result -> ResultScreen(
                        result = screen.prediction,
                        bitmap = screen.bitmap,
                        modality = screen.modality,
                        imageName = screen.imageName,
                        viewModel = viewModel
                    )
                    is Screen.History -> HistoryScreen(viewModel = viewModel)
                    is Screen.ModelInfo -> ModelInfoScreen()
                    is Screen.About -> AboutScreen()
                    is Screen.Profile -> DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }
}

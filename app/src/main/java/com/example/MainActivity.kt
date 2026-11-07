package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.YuriBackgroundDeep
import com.example.ui.viewmodel.YuriViewModel

enum class YuriScreen {
    DASHBOARD,
    CHAT,
    SETTINGS,
    TOOLS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = YuriBackgroundDeep
                ) {
                    val yuriViewModel: YuriViewModel = viewModel()
                    YuriApp(viewModel = yuriViewModel)
                }
            }
        }
    }
}

@Composable
fun YuriApp(
    viewModel: YuriViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(YuriScreen.DASHBOARD) }

    Crossfade(
        targetState = currentScreen,
        label = "ScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { screen ->
        when (screen) {
            YuriScreen.DASHBOARD -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToChat = { currentScreen = YuriScreen.CHAT },
                onNavigateToSettings = { currentScreen = YuriScreen.SETTINGS },
                onNavigateToTools = { currentScreen = YuriScreen.TOOLS }
            )
            YuriScreen.CHAT -> ChatScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = YuriScreen.DASHBOARD },
                onNavigateToSettings = { currentScreen = YuriScreen.SETTINGS }
            )
            YuriScreen.SETTINGS -> SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = YuriScreen.DASHBOARD }
            )
            YuriScreen.TOOLS -> ToolsScreen(
                viewModel = viewModel,
                onNavigateBack = { currentScreen = YuriScreen.DASHBOARD },
                onNavigateToChat = { currentScreen = YuriScreen.CHAT }
            )
        }
    }
}

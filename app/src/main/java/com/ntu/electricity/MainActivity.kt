package com.ntu.electricity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ntu.electricity.data.local.UserPreferences
import com.ntu.electricity.ui.screens.DebugWebViewScreen
import com.ntu.electricity.ui.screens.HistoryScreen
import com.ntu.electricity.ui.screens.HomeScreen
import com.ntu.electricity.ui.theme.NTUElectricityTheme
import com.ntu.electricity.viewmodel.HistoryViewModel
import com.ntu.electricity.viewmodel.HomeViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs = remember { UserPreferences(this) }
            var colorIndex by remember { mutableIntStateOf(prefs.colorIndex) }
            NTUElectricityTheme(colorIndex = colorIndex) {
                EcoCheckNavHost(
                    colorIndex = colorIndex,
                    onColorChange = { idx ->
                        colorIndex = idx
                        prefs.colorIndex = idx
                    }
                )
            }
        }
    }
}

private data class DebugParams(
    val studentId: String,
    val password: String,
    val campusId: String,
    val buildingId: String,
    val roomId: String
)

@Composable
fun EcoCheckNavHost(
    colorIndex: Int = 0,
    onColorChange: (Int) -> Unit = {}
) {
    val navController = rememberNavController()
    var debugParams by remember { mutableStateOf<DebugParams?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Open immediately, delay clearing params for exit animation
    LaunchedEffect(debugParams) {
        showDialog = debugParams != null
    }
    LaunchedEffect(showDialog) {
        if (!showDialog) {
            delay(100) // wait for exit animation (75ms + buffer)
            debugParams = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToHistory = { navController.navigate("history") },
                    onQuery = { studentId, password, campusId, buildingId, roomId ->
                        debugParams = DebugParams(studentId, password, campusId, buildingId, roomId)
                    },
                    colorIndex = colorIndex,
                    onColorChange = onColorChange
                )
            }
            composable("history",
                enterTransition = { slideInHorizontally(tween(350)) { it } },
                exitTransition = { slideOutHorizontally(tween(350)) { -it } },
                popEnterTransition = { slideInHorizontally(tween(350)) { -it } },
                popExitTransition = { slideOutHorizontally(tween(350)) { it } }
            ) {
                val historyViewModel: HistoryViewModel = viewModel()
                HistoryScreen(
                    viewModel = historyViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Dialog overlay — Material 3 standard dialog transition
        AnimatedVisibility(
            visible = showDialog,
            enter = fadeIn(tween(150, easing = LinearEasing)) +
                    scaleIn(tween(150, easing = LinearEasing), initialScale = 0.96f),
            exit = fadeOut(tween(75, easing = LinearEasing)) +
                   scaleOut(tween(75, easing = LinearEasing), targetScale = 0.96f)
        ) {
            // debugParams stays alive during exit animation
            debugParams?.let { params ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { },
                    contentAlignment = Alignment.Center
                ) {
                    DebugWebViewScreen(
                        url = "https://pay.ntu.edu.cn/innerUserLogin",
                        studentId = params.studentId,
                        password = params.password,
                        campusId = params.campusId,
                        buildingId = params.buildingId,
                        roomId = params.roomId,
                        onDismiss = { showDialog = false }
                    )
                }
            }
        }
    }
}

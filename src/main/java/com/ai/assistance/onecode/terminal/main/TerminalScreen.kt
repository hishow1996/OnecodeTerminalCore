package com.ai.assistance.onecode.terminal.main

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ai.assistance.onecode.terminal.TerminalEnv
import com.ai.assistance.onecode.terminal.TerminalManager
import com.ai.assistance.onecode.terminal.ui.SettingsScreen
import com.ai.assistance.onecode.terminal.ui.SetupScreen
import com.ai.assistance.onecode.terminal.ui.TerminalHomeV2

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TerminalScreen(env: TerminalEnv) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }
    var navigated by remember { mutableStateOf(false) }

    val manager = remember { TerminalManager.getInstance(androidx.compose.ui.platform.LocalContext.current) }
    val state by manager.terminalState.collectAsState()
    val ready = state.currentSession?.isInitializing == false

    LaunchedEffect(Unit) {
        // The shell is the product. Setup remains available from Settings,
        // but a fresh launch should land directly in Ubuntu.
        startDestination = if (env.forceShowSetup) {
            TerminalRoutes.SETUP_ROUTE
        } else {
            TerminalRoutes.TERMINAL_HOME_ROUTE
        }
    }

    NavHost(
        navController = navController,
        startDestination = "loading",
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D0E))
    ) {
        composable("loading") {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = Color(0xFF8CCF7E),
                    strokeWidth = 2.dp
                )
            }
        }
        composable(TerminalRoutes.TERMINAL_HOME_ROUTE) {
            TerminalHomeV2(
                env = env,
                onNavigateToSetup = { navController.navigate(TerminalRoutes.SETUP_ROUTE) },
                onNavigateToSettings = { navController.navigate(TerminalRoutes.SETTINGS_ROUTE) }
            )
        }
        composable(TerminalRoutes.SETUP_ROUTE) {
            SetupScreen(
                onBack = {
                    navController.navigate(TerminalRoutes.TERMINAL_HOME_ROUTE) {
                        popUpTo(TerminalRoutes.SETUP_ROUTE) { inclusive = true }
                    }
                },
                onSetup = { commands ->
                    env.onSetup(commands)
                    navController.navigate(TerminalRoutes.TERMINAL_HOME_ROUTE) {
                        popUpTo(TerminalRoutes.SETUP_ROUTE) { inclusive = true }
                    }
                }
            )
        }
        composable(TerminalRoutes.SETTINGS_ROUTE) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }

    LaunchedEffect(startDestination, ready) {
        if (startDestination != null && ready && !navigated) {
            navigated = true
            navController.navigate(startDestination!!) {
                popUpTo("loading") { inclusive = true }
            }
        }
    }
}

package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import com.example.ads.AdBanner
import com.example.ads.AdManager
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.BatchScannerScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PdfConverterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.OcrViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: OcrViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                Color.TRANSPARENT,
                Color.TRANSPARENT
            )
        )
        AdManager.initialize(applicationContext)

        setContent {
            val darkModeSetting by viewModel.darkModeSetting.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            // Default to Clean White (false) when darkModeSetting is false
            val isDark = darkModeSetting ?: systemDark
            var showSplash by remember { mutableStateOf(true) }

            MyApplicationTheme(darkTheme = isDark) {
                Crossfade(targetState = showSplash, label = "splash_transition") { isSplashVisible ->
                    if (isSplashVisible) {
                        SplashScreen(
                            onDismiss = {
                                showSplash = false
                                AdManager.showAppOpenAd(this@MainActivity)
                            }
                        )
                    } else {
                        val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

                        Scaffold(
                            modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
                            contentWindowInsets = WindowInsets.safeDrawing,
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = when (selectedTab) {
                                                0 -> "ScriptScan OCR"
                                                1 -> "Batch Scanner"
                                                2 -> "PDF Studio"
                                                3 -> "Calculator & Zakat"
                                                4 -> "Scan History"
                                                5 -> "Settings"
                                                else -> "ScriptScan OCR"
                                            },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    },
                                    actions = {
                                        // Quick 1-tap Theme Switcher in header
                                        IconButton(
                                            onClick = {
                                                viewModel.setDarkMode(!isDark)
                                            },
                                            modifier = Modifier.testTag("btn_quick_theme_toggle")
                                        ) {
                                            Icon(
                                                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                                                contentDescription = if (isDark) "Switch to Light Yellow Theme" else "Switch to Dark Theme",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        titleContentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.testTag("app_top_bar")
                                )
                            },
                            bottomBar = {
                                Column(modifier = Modifier.testTag("bottom_bar_container")) {
                                    AdBanner()
                                    NavigationBar(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("bottom_navigation_bar")
                                    ) {
                                        NavigationBarItem(
                                            selected = selectedTab == 0,
                                            onClick = { viewModel.setTab(0) },
                                            icon = { Icon(Icons.Default.DocumentScanner, contentDescription = "Scan & OCR") },
                                            label = { Text("Scan") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            modifier = Modifier.testTag("nav_tab_scan")
                                        )

                                        NavigationBarItem(
                                            selected = selectedTab == 1,
                                            onClick = { viewModel.setTab(1) },
                                            icon = { Icon(Icons.Default.Layers, contentDescription = "Batch Scanner") },
                                            label = { Text("Batch") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.secondary,
                                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                            modifier = Modifier.testTag("nav_tab_batch")
                                        )

                                        NavigationBarItem(
                                            selected = selectedTab == 2,
                                            onClick = { viewModel.setTab(2) },
                                            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Studio") },
                                            label = { Text("PDF") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.tertiary,
                                                indicatorColor = MaterialTheme.colorScheme.tertiaryContainer
                                            ),
                                            modifier = Modifier.testTag("nav_tab_pdf")
                                        )

                                        NavigationBarItem(
                                            selected = selectedTab == 3,
                                            onClick = { viewModel.setTab(3) },
                                            icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculator & Zakat") },
                                            label = { Text("Calc") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            modifier = Modifier.testTag("nav_tab_calculator")
                                        )

                                        NavigationBarItem(
                                            selected = selectedTab == 4,
                                            onClick = { viewModel.setTab(4) },
                                            icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                            label = { Text("History") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            modifier = Modifier.testTag("nav_tab_history")
                                        )

                                        NavigationBarItem(
                                            selected = selectedTab == 5,
                                            onClick = { viewModel.setTab(5) },
                                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                            label = { Text("Settings") },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            modifier = Modifier.testTag("nav_tab_settings")
                                        )
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (selectedTab) {
                                    0 -> HomeScreen(viewModel = viewModel)
                                    1 -> BatchScannerScreen(viewModel = viewModel)
                                    2 -> PdfConverterScreen(viewModel = viewModel)
                                    3 -> CalculatorScreen(viewModel = viewModel)
                                    4 -> HistoryScreen(viewModel = viewModel)
                                    5 -> SettingsScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

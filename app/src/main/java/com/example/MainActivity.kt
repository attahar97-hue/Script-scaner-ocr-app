package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PdfConverterScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.OcrViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: OcrViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val darkModeSetting by viewModel.darkModeSetting.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = darkModeSetting ?: systemDark

            MyApplicationTheme(darkTheme = isDark) {
                val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = when (selectedTab) {
                                        0 -> "ScriptScan OCR"
                                        1 -> "PDF to Text Studio"
                                        2 -> "Scan History"
                                        3 -> "Settings & Preferences"
                                        else -> "ScriptScan OCR"
                                    },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("app_top_bar")
                        )
                    },
                    bottomBar = {
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
                                icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Studio") },
                                label = { Text("PDF") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.secondary,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                modifier = Modifier.testTag("nav_tab_pdf")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { viewModel.setTab(2) },
                                icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                label = { Text("History") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.tertiary,
                                    indicatorColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                modifier = Modifier.testTag("nav_tab_history")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { viewModel.setTab(3) },
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
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> HomeScreen(viewModel = viewModel)
                            1 -> PdfConverterScreen(viewModel = viewModel)
                            2 -> HistoryScreen(viewModel = viewModel)
                            3 -> SettingsScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

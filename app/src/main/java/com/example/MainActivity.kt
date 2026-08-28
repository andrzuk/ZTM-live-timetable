package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.NearMe
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NearbyStopsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.StopDetailScreen
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.M3Background
import com.example.ui.theme.M3Border
import com.example.ui.theme.M3Surface
import com.example.ui.theme.M3SurfaceElevated
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PolishPrimary
import com.example.ui.theme.PolishSecondaryContainer
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.TransitViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainTransitApp()
            }
        }
    }
}

@Composable
fun MainTransitApp(
    viewModel: TransitViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.refreshUserLocation()
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.refreshUserLocation()
        }
    }

    BackHandler(enabled = uiState.currentScreen != AppScreen.HOME) {
        viewModel.navigateTo(AppScreen.HOME)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (uiState.currentScreen != AppScreen.STOP_DETAILS) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar"),
                    containerColor = M3Surface,
                    contentColor = PolishPrimary,
                    tonalElevation = 6.dp
                ) {
                    NavigationBarItem(
                        selected = uiState.currentScreen == AppScreen.HOME,
                        onClick = { viewModel.navigateTo(AppScreen.HOME) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.currentScreen == AppScreen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Główny"
                            )
                        },
                        label = {
                            Text(
                                text = "Główny",
                                fontSize = 11.sp,
                                fontWeight = if (uiState.currentScreen == AppScreen.HOME) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PolishPrimary,
                            selectedTextColor = PolishPrimary,
                            indicatorColor = PolishSecondaryContainer,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_home")
                    )

                    NavigationBarItem(
                        selected = uiState.currentScreen == AppScreen.SEARCH,
                        onClick = { viewModel.navigateTo(AppScreen.SEARCH) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.currentScreen == AppScreen.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                                contentDescription = "Szukaj"
                            )
                        },
                        label = {
                            Text(
                                text = "Przystanki",
                                fontSize = 11.sp,
                                fontWeight = if (uiState.currentScreen == AppScreen.SEARCH) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PolishPrimary,
                            selectedTextColor = PolishPrimary,
                            indicatorColor = PolishSecondaryContainer,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_search")
                    )

                    NavigationBarItem(
                        selected = uiState.currentScreen == AppScreen.NEARBY,
                        onClick = { viewModel.navigateTo(AppScreen.NEARBY) },
                        icon = {
                            Icon(
                                imageVector = if (uiState.currentScreen == AppScreen.NEARBY) Icons.Filled.NearMe else Icons.Outlined.NearMe,
                                contentDescription = "W pobliżu"
                            )
                        },
                        label = {
                            Text(
                                text = "Lokalizacja",
                                fontSize = 11.sp,
                                fontWeight = if (uiState.currentScreen == AppScreen.NEARBY) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PolishPrimary,
                            selectedTextColor = PolishPrimary,
                            indicatorColor = PolishSecondaryContainer,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("nav_nearby")
                    )
                }
            }
        },
        containerColor = M3Background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(M3Background)
        ) {
            AnimatedContent(
                targetState = uiState.currentScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                    AppScreen.SEARCH -> SearchScreen(viewModel = viewModel)
                    AppScreen.NEARBY -> NearbyStopsScreen(viewModel = viewModel)
                    AppScreen.STOP_DETAILS -> StopDetailScreen(viewModel = viewModel)
                }
            }
        }
    }
}

package com.dodhi

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dodhi.ui.theme.DodhiTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dodhi.ui.screens.*
import com.dodhi.ui.viewmodel.DashboardViewModel
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup Media Player using the file from raw resource
        try {
            // Check if resource exists in raw
            val resId = resources.getIdentifier("background_sound", "raw", packageName)
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(this, resId)
                mediaPlayer?.apply {
                    isLooping = true
                    setVolume(0.1f, 0.1f) // Keep sound low and calm
                }
            } else {
                Log.w("MainActivity", "background_sound not found in raw resources")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading background sound: ${e.message}")
        }

        setContent {
            DodhiTheme {
                val navController = rememberNavController()
                val viewModel: DashboardViewModel = viewModel()
                val isMusicEnabled by viewModel.isMusicEnabled.collectAsState()
                
                // Manage music playback based on lifecycle AND settings
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, isMusicEnabled) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (isMusicEnabled) {
                            when (event) {
                                Lifecycle.Event.ON_RESUME -> {
                                    if (mediaPlayer?.isPlaying == false) mediaPlayer?.start()
                                }
                                Lifecycle.Event.ON_PAUSE -> {
                                    mediaPlayer?.pause()
                                }
                                else -> {}
                            }
                        } else {
                            mediaPlayer?.pause()
                        }
                    }
                    
                    if (isMusicEnabled && lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
                        if (mediaPlayer?.isPlaying == false) mediaPlayer?.start()
                    } else {
                        mediaPlayer?.pause()
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        mediaPlayer?.pause()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController, startDestination = "splash") {
                        composable("splash") {
                            SplashScreen(onTimeout = {
                                navController.navigate("dashboard") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            })
                        }
                        composable("dashboard") { 
                            DashboardScreen(
                                viewModel = viewModel,
                                onAllTimeReportsClick = { navController.navigate("all_time_reports") },
                                onReportsClick = { navController.navigate("report") },
                                onAddMemberClick = { navController.navigate("add_customer") },
                                onDailyRunClick = { navController.navigate("daily_run") },
                                onCustomerClick = { customerId -> navController.navigate("customer_detail/$customerId") },
                                onAboutClick = { navController.navigate("about") }
                            ) 
                        }
                        composable("about") { AboutScreen(onBack = { navController.popBackStack() }) }
                        composable("all_time_reports") {
                            AllTimeReportsScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("add_customer") { AddCustomerScreen(viewModel) { navController.popBackStack() } }
                        composable("report") { ReportScreen(viewModel) { customerId -> navController.navigate("customer_detail/$customerId") } }
                        composable("daily_run") { DailyRunScreen(viewModel) { navController.popBackStack() } }
                        composable(
                            "customer_detail/{customerId}",
                            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
                        ) { backStackEntry ->
                            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
                            CustomerDetailScreen(viewModel, customerId) { navController.popBackStack() }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

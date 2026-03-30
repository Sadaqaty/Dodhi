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

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DodhiTheme {
                val navController = rememberNavController()
                val viewModel: DashboardViewModel = viewModel()
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController, startDestination = "dashboard") {
                        composable("dashboard") { 
                            DashboardScreen(
                                viewModel = viewModel,
                                onMilkCollectionClick = { navController.navigate("daily_entry") },
                                onReportsClick = { navController.navigate("report") },
                                onAddMemberClick = { navController.navigate("add_customer") },
                                onMorningRunClick = { navController.navigate("morning_run") }
                            ) 
                        }
                        composable("daily_entry") {
                            DailyEntryScreen(
                                viewModel = viewModel,
                                onCustomerClick = { customerId -> navController.navigate("customer_detail/$customerId") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("add_customer") { AddCustomerScreen(viewModel) { navController.popBackStack() } }
                        composable("report") { ReportScreen(viewModel) { customerId -> navController.navigate("customer_detail/$customerId") } }
                        composable("morning_run") { MorningRunScreen(viewModel) { navController.popBackStack() } }
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
}

package com.dodhi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dodhi.ui.theme.DodhiTheme
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
import com.dodhi.ui.theme.GoldDark
import com.dodhi.ui.theme.CreamBase
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dodhi.ui.screens.DashboardScreen
import com.dodhi.ui.screens.AddCustomerScreen
import com.dodhi.ui.screens.ReportScreen
import com.dodhi.ui.screens.CustomerDetailScreen
import com.dodhi.ui.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DodhiTheme {
                val navController = rememberNavController()
                val viewModel: DashboardViewModel = viewModel()
                
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 8.dp
                        ) {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route

                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = null, tint = if (currentRoute == "dashboard") GoldDark else Color.Gray) },
                                label = { Text(stringResource(R.string.daily_entry), color = if (currentRoute == "dashboard") GoldDark else Color.Gray, fontWeight = FontWeight.Bold) },
                                selected = currentRoute == "dashboard",
                                onClick = { navController.navigate("dashboard") },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = CreamBase)
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Add, contentDescription = null, tint = if (currentRoute == "add_customer") GoldDark else Color.Gray) },
                                label = { Text(stringResource(R.string.add_customer), color = if (currentRoute == "add_customer") GoldDark else Color.Gray, fontWeight = FontWeight.Bold) },
                                selected = currentRoute == "add_customer",
                                onClick = { navController.navigate("add_customer") },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = CreamBase)
                            )
                             NavigationBarItem(
                                icon = { Icon(Icons.Default.List, contentDescription = null, tint = if (currentRoute == "report") GoldDark else Color.Gray) },
                                label = { Text(stringResource(R.string.monthly_report), color = if (currentRoute == "report") GoldDark else Color.Gray, fontWeight = FontWeight.Bold) },
                                selected = currentRoute == "report",
                                onClick = { navController.navigate("report") },
                                colors = NavigationBarItemDefaults.colors(indicatorColor = CreamBase)
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(navController, startDestination = "dashboard", Modifier.padding(innerPadding)) {
                        composable("dashboard") { DashboardScreen(viewModel) { customerId -> navController.navigate("customer_detail/$customerId") } }
                        composable("add_customer") { AddCustomerScreen(viewModel) { navController.popBackStack() } }
                        composable("report") { ReportScreen(viewModel) { customerId -> navController.navigate("customer_detail/$customerId") } }
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

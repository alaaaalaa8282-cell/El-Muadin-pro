package com.AbuAlaa.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.AbuAlaa.ui.screens.AzkarScreen
import com.AbuAlaa.ui.screens.CalendarScreen
import com.AbuAlaa.ui.screens.HomeScreen
import com.AbuAlaa.ui.screens.QiblaScreen
import com.AbuAlaa.ui.screens.SettingsScreen
import com.AbuAlaa.ui.widgets.AppGradientBackground
import com.AbuAlaa.noor.ui.NoorScreen
import com.AbuAlaa.noor.ui.ModelsScreen

sealed class BottomDest(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    data object Home : BottomDest("home", "الصلاة", Icons.Filled.Mosque)
    data object Calendar : BottomDest("calendar", "التقويم", Icons.Filled.CalendarMonth)
    data object Qibla : BottomDest("qibla", "القبلة", Icons.Filled.Explore)
    data object Azkar : BottomDest("azkar", "الأذكار", Icons.AutoMirrored.Filled.MenuBook)
   // data object Noor : BottomDest("noor", "نور", Icons.Filled.Chat) // New Tab
    data object Settings : BottomDest("settings", "الضبط", Icons.Filled.Settings)
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val items = listOf(
        BottomDest.Home,
        BottomDest.Calendar,
        BottomDest.Qibla,
        BottomDest.Azkar,
       // BottomDest.Noor, // Add to bottom bar
        BottomDest.Settings
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                items.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = BottomDest.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(BottomDest.Home.route) {
                HomeScreen(
                    contentPadding = padding,
                    onOpenAzkar = { navController.navigate(BottomDest.Azkar.route) }
                )
            }
            composable(BottomDest.Calendar.route) {
                CalendarScreen(contentPadding = padding)
            }
            composable(BottomDest.Qibla.route) {
                QiblaScreen(contentPadding = padding)
            }
            composable(BottomDest.Azkar.route) {
                AzkarScreen(contentPadding = padding)
            }
           // composable(BottomDest.Noor.route) {
             //   NoorScreen(
               //     contentPadding = padding,
                //    onSettingsClick = { navController.navigate(BottomDest.Settings.route) },
                //    onModelsClick = { navController.navigate("noor_models") }
             //   )
         //   }
        //    composable("noor_models") {
         //       ModelsScreen(
          //          onBackClick = { navController.popBackStack() },
           //         onModelSelect = { modelId ->
                        // TODO: Update selected model in NoorViewModel
             //           navController.popBackStack()
            //        },
              //      selectedModelId = "Omni" // TODO: Get from NoorViewModel
         //       )
         //   }
            composable(BottomDest.Settings.route) {
                SettingsScreen(contentPadding = padding)
            }
        }
    }
}

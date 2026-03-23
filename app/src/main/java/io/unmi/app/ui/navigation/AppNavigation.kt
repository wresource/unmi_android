package io.unmi.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import io.unmi.app.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import io.unmi.app.ui.about.AboutScreen
import io.unmi.app.ui.domain.detail.DomainDetailScreen
import io.unmi.app.ui.domain.edit.DomainEditScreen
import io.unmi.app.ui.domain.list.DomainListScreen
import io.unmi.app.ui.home.HomeScreen
import io.unmi.app.ui.init.InitScreen
import io.unmi.app.ui.settings.SettingsScreen
import io.unmi.app.ui.statistics.StatisticsScreen
import io.unmi.app.ui.tools.ToolsScreen
import io.unmi.app.ui.unlock.UnlockScreen
import io.unmi.app.ui.whois.WhoisScreen

data class BottomNavItem(
    val labelResId: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val route: String
)

val bottomNavItems = listOf(
    BottomNavItem(R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home, AppRoute.Home.route),
    BottomNavItem(R.string.domain_list_title, Icons.Outlined.Language, Icons.Filled.Language, AppRoute.DomainList.route),
    BottomNavItem(R.string.stats_title, Icons.Outlined.BarChart, Icons.Filled.BarChart, AppRoute.Statistics.route),
    BottomNavItem(R.string.tools_title, Icons.Outlined.Handyman, Icons.Filled.Handyman, AppRoute.Tools.route),
    BottomNavItem(R.string.settings_title, Icons.Outlined.Settings, Icons.Filled.Settings, AppRoute.Settings.route)
)

@Composable
fun AppNavigation(
    startDestination: String
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.hierarchy?.any { dest ->
        bottomNavItems.any { it.route == dest.route }
    } == true

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        val label = stringResource(item.labelResId)
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    if (isSelected) item.selectedIcon else item.icon,
                                    contentDescription = label
                                )
                            },
                            label = { Text(label) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Unlock.route) {
                UnlockScreen(
                    onUnlocked = {
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.Unlock.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(AppRoute.Init.route) {
                InitScreen(
                    onInitComplete = {
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.Init.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(AppRoute.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(AppRoute.DomainList.route) {
                DomainListScreen(navController = navController)
            }
            composable(
                route = AppRoute.DomainDetail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val domainId = backStackEntry.arguments?.getLong("id") ?: 0L
                DomainDetailScreen(
                    domainId = domainId,
                    navController = navController
                )
            }
            composable(
                route = AppRoute.DomainEdit.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val domainId = backStackEntry.arguments?.getLong("id") ?: 0L
                DomainEditScreen(
                    domainId = domainId,
                    navController = navController
                )
            }
            composable(AppRoute.DomainCreate.route) {
                DomainEditScreen(
                    domainId = null,
                    navController = navController
                )
            }
            composable(AppRoute.Statistics.route) {
                StatisticsScreen(navController = navController)
            }
            composable(AppRoute.Tools.route) {
                ToolsScreen(navController = navController)
            }
            composable(AppRoute.Whois.route) {
                WhoisScreen(navController = navController)
            }
            composable(AppRoute.Settings.route) {
                SettingsScreen(navController = navController)
            }
            composable(AppRoute.About.route) {
                AboutScreen(navController = navController)
            }
        }
    }
}

package com.medicare.ai.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.medicare.ai.*
import com.medicare.ai.ui.screens.*
import com.medicare.ai.ui.theme.MediCareTheme

object Route {
    const val SPLASH = "splash"; const val ONBOARD = "onboarding"; const val AUTH = "auth"
    const val HOME = "home"; const val CHAT = "chat"; const val MEDS = "medicines"; const val APPTS = "appointments"
    const val MORE = "more"; const val WATER = "water"; const val SLEEP = "sleep"; const val STEPS = "activity"
    const val SYMPTOMS = "symptoms"; const val PROFILE = "profile"; const val HISTORY = "history"
    const val EMERGENCY = "emergency"; const val SETTINGS = "settings"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)
private val tabs = listOf(
    Tab(Route.HOME, "Home", Icons.Filled.Home), Tab(Route.CHAT, "Assistant", Icons.Filled.ChatBubble),
    Tab(Route.MEDS, "Medicines", Icons.Filled.Medication), Tab(Route.APPTS, "Visits", Icons.Filled.Event),
    Tab(Route.MORE, "More", Icons.Filled.Menu)
)

fun NavController.go(route: String) = navigate(route) { launchSingleTop = true }

@Composable
fun MediCareRoot(c: AppContainer) {
    val theme by c.prefs.theme.collectAsStateWithLifecycle(0)
    val dark = when (theme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    MediCareTheme(dark) {
        val nav = rememberNavController()
        val f = remember { VMFactory(c) }
        val route = nav.currentBackStackEntryAsState().value?.destination?.route
        val user by c.auth.userFlow.collectAsStateWithLifecycle(c.auth.currentUser)

        // If the session ends anywhere in the app, go back to login
        LaunchedEffect(user, route) {
            if (user == null && route != null && route !in setOf(Route.SPLASH, Route.ONBOARD, Route.AUTH))
                nav.navigate(Route.AUTH) { popUpTo(0) }
        }

        Scaffold(bottomBar = {
            if (route in tabs.map { it.route }) NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(selected = route == t.route, label = { Text(t.label) },
                        icon = { Icon(t.icon, null) }, onClick = {
                            nav.navigate(t.route) { popUpTo(Route.HOME) { saveState = true }; launchSingleTop = true; restoreState = true }
                        })
                }
            }
        }) { pad ->
            NavHost(nav, Route.SPLASH, Modifier.padding(pad)) {
                composable(Route.SPLASH) { SplashScreen(c) { r -> nav.navigate(r) { popUpTo(Route.SPLASH) { inclusive = true } } } }
                composable(Route.ONBOARD) { OnboardingScreen(c) { nav.navigate(Route.AUTH) { popUpTo(Route.ONBOARD) { inclusive = true } } } }
                composable(Route.AUTH) { AuthScreen(viewModel(factory = f)) { nav.navigate(Route.HOME) { popUpTo(Route.AUTH) { inclusive = true } } } }
                composable(Route.HOME) { HomeScreen(nav, viewModel(factory = f), viewModel(factory = f), viewModel(factory = f), viewModel(factory = f)) }
                composable(Route.MORE) { MoreScreen(nav) }
                composable(Route.CHAT) { ChatScreen(nav, viewModel(factory = f)) }
                composable(Route.MEDS) { MedicineScreen(nav, viewModel(factory = f)) }
                composable(Route.APPTS) { AppointmentScreen(nav, viewModel(factory = f)) }
                composable(Route.WATER) { TrackerScreen(nav, viewModel(factory = f), "WATER") }
                composable(Route.SLEEP) { TrackerScreen(nav, viewModel(factory = f), "SLEEP") }
                composable(Route.STEPS) { TrackerScreen(nav, viewModel(factory = f), "STEPS") }
                composable(Route.SYMPTOMS) { SymptomScreen(nav) }
                composable(Route.PROFILE) { ProfileScreen(nav, viewModel(factory = f)) }
                composable(Route.HISTORY) { HistoryScreen(nav, viewModel(factory = f), viewModel(factory = f), viewModel(factory = f), viewModel(factory = f)) }
                composable(Route.EMERGENCY) { EmergencyScreen(nav, viewModel(factory = f)) }
                composable(Route.SETTINGS) { SettingsScreen(nav, viewModel(factory = f)) }
            }
        }
    }
}
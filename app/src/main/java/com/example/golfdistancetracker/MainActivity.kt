package com.example.golfdistancetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.*
import com.example.golfdistancetracker.auth.AuthManager
import com.example.golfdistancetracker.data.prefs.ThemePreference
import com.example.golfdistancetracker.ui.screen.*
import com.example.golfdistancetracker.ui.theme.GolfDistanceTrackerTheme
import com.example.golfdistancetracker.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val themePref by settingsViewModel.themePreference.collectAsState()

            val darkTheme = when (themePref) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            GolfDistanceTrackerTheme(darkTheme = darkTheme) {
                MainApp(authManager)
            }
        }
    }
}

@Composable
fun MainApp(authManager: AuthManager) {
    val navController = rememberNavController()
    val user by authManager.userState.collectAsState()

    if (user == null) {
        LoginScreen(authManager) {
            // Callback handled by state change
        }
    } else {
        Scaffold(
            bottomBar = {
                    NavigationBar {
                        val currentBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = currentBackStackEntry?.destination?.route

                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_home)) },
                            selected = currentRoute == "dashboard",
                            onClick = { navController.navigate("dashboard") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Adjust, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_field)) },
                            selected = currentRoute == "session",
                            onClick = { navController.navigate("session") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.SportsGolf, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_practice)) },
                            selected = currentRoute == "practice",
                            onClick = { navController.navigate("practice") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_bag)) },
                            selected = currentRoute == "bag",
                            onClick = { navController.navigate("bag") }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_stats)) },
                            selected = currentRoute == "stats",
                            onClick = { navController.navigate("stats") }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable("dashboard") { 
                        DashboardScreen(
                            statsViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
                            onNavigate = { route -> navController.navigate(route) }
                        ) 
                    }
                    composable("session") { SessionScreen() }
                    composable("practice") { DrivingRangeScreen() }
                    composable("courses") { CourseManagementScreen() }
                    composable("bag") { GolfBagScreen() }
                    composable("stats") { StatsScreen() }
                    composable("scorecard") { ScorecardScreen() }
                    composable("settings") { SettingsScreen() }
                }
            }
    }
}

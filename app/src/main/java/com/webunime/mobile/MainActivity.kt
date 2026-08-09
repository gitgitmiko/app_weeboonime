package com.webunime.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.webunime.mobile.ui.calendar.CalendarScreen
import com.webunime.mobile.ui.detail.DetailScreen
import com.webunime.mobile.ui.home.HomeScreen
import com.webunime.mobile.ui.search.SearchScreen
import com.webunime.mobile.ui.theme.WebunimeTheme
import com.webunime.mobile.ui.update.AppUpdateHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebunimeTheme {
                AppUpdateHost(activity = this@MainActivity)

                val nav = rememberNavController()
                val backStack by nav.currentBackStackEntryAsState()
                val route = backStack?.destination?.route.orEmpty()
                val showBottom = route in setOf("home", "search", "calendar")

                Scaffold(
                    bottomBar = {
                        if (showBottom) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = route == "home",
                                    onClick = {
                                        nav.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = { Icon(Icons.Default.Home, null) },
                                    label = { Text("Home") },
                                )
                                NavigationBarItem(
                                    selected = route == "search",
                                    onClick = {
                                        nav.navigate("search") { launchSingleTop = true }
                                    },
                                    icon = { Icon(Icons.Default.Search, null) },
                                    label = { Text("Cari") },
                                )
                                NavigationBarItem(
                                    selected = route == "calendar",
                                    onClick = {
                                        nav.navigate("calendar") { launchSingleTop = true }
                                    },
                                    icon = { Icon(Icons.Default.CalendarMonth, null) },
                                    label = { Text("Jadwal") },
                                )
                            }
                        }
                    },
                ) { padding ->
                    NavHost(
                        navController = nav,
                        startDestination = "home",
                        modifier = Modifier.padding(padding),
                    ) {
                        composable("home") {
                            HomeScreen(
                                onOpenAnime = { slug -> nav.navigate("detail/$slug") },
                            )
                        }
                        composable("search") {
                            SearchScreen(
                                onOpenAnime = { slug -> nav.navigate("detail/$slug") },
                            )
                        }
                        composable("calendar") {
                            CalendarScreen(
                                onOpenAnime = { slug -> nav.navigate("detail/$slug") },
                            )
                        }
                        composable(
                            route = "detail/{slug}",
                            arguments = listOf(navArgument("slug") { type = NavType.StringType }),
                        ) { entry ->
                            val slug = entry.arguments?.getString("slug").orEmpty()
                            DetailScreen(
                                slug = slug,
                                onBack = { nav.popBackStack() },
                            )
                        }
                    }
                }
            }
        }
    }
}

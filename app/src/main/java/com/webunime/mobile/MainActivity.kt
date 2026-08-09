package com.webunime.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebunimeTheme {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(1800)
                    showSplash = false
                }

                Box(Modifier.fillMaxSize()) {
                    AppUpdateHost(activity = this@MainActivity)

                    val nav = rememberNavController()
                    val backStack by nav.currentBackStackEntryAsState()
                    val route = backStack?.destination?.route.orEmpty()
                    val showBottom = route in setOf("home", "search", "calendar")

                    Scaffold(
                        bottomBar = {
                            if (showBottom && !showSplash) {
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

                    if (showSplash) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                            contentAlignment = Alignment.Center,
                        ) {
                            Image(
                                painter = painterResource(R.drawable.logo_transparan),
                                contentDescription = "Weeboonime",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth(0.78f)
                                    .padding(horizontal = 24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

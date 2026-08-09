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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.webunime.mobile.ui.calendar.CalendarScreen
import com.webunime.mobile.ui.detail.DetailScreen
import com.webunime.mobile.ui.history.HistoryScreen
import com.webunime.mobile.ui.home.HomeScreen
import com.webunime.mobile.ui.search.SearchScreen
import com.webunime.mobile.ui.subscribed.SubscribedScreen
import com.webunime.mobile.ui.theme.WebunimeTheme
import com.webunime.mobile.ui.timeline.TimelineScreen
import com.webunime.mobile.ui.update.AppUpdateHost
import kotlinx.coroutines.delay

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/** Urutan tab ala Wibuku: Home → Jadwal → History → Subscribed → Timeline */
private val bottomTabs = listOf(
    BottomTab("home", "Home", Icons.Filled.Home),
    BottomTab("schedule", "Jadwal", Icons.Outlined.DateRange),
    BottomTab("history", "History", Icons.Rounded.AccessTime),
    BottomTab("subscribed", "Subscribed", Icons.Rounded.Subscriptions),
    BottomTab("timeline", "Timeline", Icons.Filled.People),
)

private val bottomBarBg = Color(0xFF17181A)

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
                    val showBottom = route in bottomTabs.map { it.route }

                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        bottomBar = {
                            if (showBottom && !showSplash) {
                                NavigationBar(
                                    containerColor = bottomBarBg,
                                    contentColor = Color.White,
                                ) {
                                    bottomTabs.forEach { tab ->
                                        NavigationBarItem(
                                            selected = route == tab.route,
                                            onClick = {
                                                nav.navigate(tab.route) {
                                                    popUpTo(nav.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                                            label = { Text(tab.label) },
                                            alwaysShowLabel = false,
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = Color.White,
                                                selectedTextColor = Color.White,
                                                unselectedIconColor = Color(0xFFA0A0A1),
                                                unselectedTextColor = Color(0xFFA0A0A1),
                                                indicatorColor = Color(0xFF2B2C2F),
                                            ),
                                        )
                                    }
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
                                    onOpenSearch = { nav.navigate("search") },
                                )
                            }
                            composable("schedule") {
                                CalendarScreen(
                                    onOpenAnime = { slug -> nav.navigate("detail/$slug") },
                                )
                            }
                            composable("history") {
                                HistoryScreen(
                                    onOpenAnime = { slug -> nav.navigate("detail/$slug") },
                                )
                            }
                            composable("subscribed") {
                                SubscribedScreen()
                            }
                            composable("timeline") {
                                TimelineScreen()
                            }
                            composable("search") {
                                SearchScreen(
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

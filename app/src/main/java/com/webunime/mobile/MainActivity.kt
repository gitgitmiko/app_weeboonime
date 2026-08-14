package com.webunime.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.webunime.mobile.data.fcm.EpisodeNotify
import com.webunime.mobile.data.putEpisodeExtra
import com.webunime.mobile.ui.account.AccountScreen
import com.webunime.mobile.ui.auth.AuthOnboardingFlow
import com.webunime.mobile.ui.auth.LegalDoc
import com.webunime.mobile.ui.auth.LegalScreen
import com.webunime.mobile.ui.calendar.CalendarScreen
import com.webunime.mobile.ui.detail.DetailScreen
import com.webunime.mobile.ui.history.HistoryScreen
import com.webunime.mobile.ui.home.HomeScreen
import com.webunime.mobile.ui.player.MiniPlayerBar
import com.webunime.mobile.ui.player.PlayerActivity
import com.webunime.mobile.ui.premium.PremiumPackageScreen
import com.webunime.mobile.ui.search.SearchScreen
import com.webunime.mobile.ui.subscribed.SubscribedScreen
import com.webunime.mobile.ui.theme.WebunimeTheme
import com.webunime.mobile.ui.theme.WuColors
import com.webunime.mobile.ui.timeline.TimelineScreen
import com.webunime.mobile.ui.update.AppUpdateHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    BottomTab("subscribed", "Subscribed", Icons.Filled.VideoLibrary),
    BottomTab("timeline", "Timeline", Icons.Filled.Person),
)

private val authBg = Color(0xFF151719)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebunimeTheme {
                val app = LocalContext.current.applicationContext as WebunimeApp
                val activity = this@MainActivity
                val scope = rememberCoroutineScope()
                var showSplash by remember { mutableStateOf(true) }
                var loggedIn by remember {
                    mutableStateOf(app.session.isLoggedIn || app.authRepository.isSignedIn())
                }

                var loginBusy by remember { mutableStateOf(false) }
                var pendingOpenSlug by remember {
                    mutableStateOf(intent.getStringExtra(EpisodeNotify.EXTRA_OPEN_SLUG))
                }

                DisposableEffect(Unit) {
                    val listener = androidx.core.util.Consumer<Intent> { newIntent ->
                        pendingOpenSlug = newIntent.getStringExtra(EpisodeNotify.EXTRA_OPEN_SLUG)
                    }
                    addOnNewIntentListener(listener)
                    onDispose { removeOnNewIntentListener(listener) }
                }

                val notifyPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { /* ignore; user can enable later in settings */ }

                LaunchedEffect(loggedIn) {
                    if (!loggedIn) return@LaunchedEffect
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val googleSignInLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { result ->
                    scope.launch {
                        try {
                            if (result.resultCode != android.app.Activity.RESULT_OK) {
                                loginBusy = false
                                Toast.makeText(
                                    activity,
                                    "Login Google dibatalkan",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@launch
                            }
                            val res = app.authRepository.handleGoogleSignInResult(result.data)
                            res.onSuccess {
                                val user = app.authRepository.currentUser
                                app.session.loginAs(
                                    user?.displayName ?: user?.email ?: "Google User",
                                )
                                // Jangan blokir UI: sync Firestore di background
                                loggedIn = true
                                loginBusy = false
                                Toast.makeText(
                                    activity,
                                    "Login berhasil",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                launch {
                                    val uid = app.authRepository.currentUser?.uid
                                    runCatching {
                                        app.bindSignedInUser(uid)
                                        val subs = app.userRepository.current().animeSubs
                                        com.webunime.mobile.data.fcm.FcmTopicManager.syncTopics(
                                            subs,
                                            emptyList(),
                                        )
                                    }.onFailure {
                                        android.util.Log.w(
                                            "WebunimeAuth",
                                            "Sync cloud gagal (lokal tetap jalan): ${it.message}",
                                        )
                                    }
                                }
                            }.onFailure {
                                loginBusy = false
                                Toast.makeText(
                                    activity,
                                    it.message ?: "Login Google gagal",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        } catch (t: Throwable) {
                            loginBusy = false
                            Toast.makeText(
                                activity,
                                t.message ?: "Login error",
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    delay(1800)
                    showSplash = false
                }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(if (loggedIn) MaterialTheme.colorScheme.background else authBg),
                ) {
                    AppUpdateHost(activity = this@MainActivity)

                    when {
                        showSplash -> {
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

                        !loggedIn -> {
                            val authNav = rememberNavController()
                            NavHost(
                                navController = authNav,
                                startDestination = "onboarding",
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                composable("onboarding") {
                                    AuthOnboardingFlow(
                                        onGoogleLogin = {
                                            if (loginBusy) return@AuthOnboardingFlow
                                            scope.launch {
                                                loginBusy = true
                                                app.authRepository.prepareSignIn(activity)
                                                val intent = app.authRepository.signInIntent(activity)
                                                if (intent == null) {
                                                    loginBusy = false
                                                    Toast.makeText(
                                                        activity,
                                                        "Cek GOOGLE_WEB_CLIENT_ID / google-services.json",
                                                        Toast.LENGTH_LONG,
                                                    ).show()
                                                } else {
                                                    googleSignInLauncher.launch(intent)
                                                }
                                            }
                                        },
                                        onTesterLogin = {
                                            app.session.loginAs("Tester")
                                            loggedIn = true
                                        },
                                        onOpenPrivacy = { authNav.navigate("privacy") },
                                        onOpenTerms = { authNav.navigate("terms") },
                                    )
                                }
                                composable("privacy") {
                                    LegalScreen(
                                        doc = LegalDoc.Privacy,
                                        onBack = { authNav.popBackStack() },
                                    )
                                }
                                composable("terms") {
                                    LegalScreen(
                                        doc = LegalDoc.Terms,
                                        onBack = { authNav.popBackStack() },
                                    )
                                }
                            }
                        }

                        else -> {
                            val nav = rememberNavController()
                            val backStack by nav.currentBackStackEntryAsState()
                            val route = backStack?.destination?.route.orEmpty()
                            val showBottom = route in bottomTabs.map { it.route }
                            val nowPlaying by app.nowPlaying.current.collectAsStateWithLifecycle()

                            LaunchedEffect(pendingOpenSlug, loggedIn) {
                                val slug = pendingOpenSlug?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
                                if (!loggedIn) return@LaunchedEffect
                                nav.navigate("detail/$slug") {
                                    launchSingleTop = true
                                }
                                pendingOpenSlug = null
                            }

                            Scaffold(
                                containerColor = WuColors.Bg,
                                bottomBar = {
                                    Column {
                                        nowPlaying?.let { playing ->
                                            MiniPlayerBar(
                                                playing = playing,
                                                onOpen = {
                                                    app.nowPlaying.clear()
                                                    val i = Intent(activity, PlayerActivity::class.java).apply {
                                                        putExtra(PlayerActivity.EXTRA_SLUG, playing.slug)
                                                        putEpisodeExtra(PlayerActivity.EXTRA_EPISODE, playing.episode)
                                                        putExtra(PlayerActivity.EXTRA_TITLE, playing.title)
                                                        putExtra(PlayerActivity.EXTRA_THUMBNAIL, playing.thumbnail)
                                                    }
                                                    activity.startActivity(i)
                                                },
                                                onClose = { app.nowPlaying.clear() },
                                            )
                                        }
                                        if (showBottom) {
                                            NavigationBar(
                                                containerColor = WuColors.NavBar,
                                                contentColor = Color.White,
                                                tonalElevation = 0.dp,
                                            ) {
                                                bottomTabs.forEach { tab ->
                                                    val selected = route == tab.route
                                                    NavigationBarItem(
                                                        selected = selected,
                                                        onClick = {
                                                            nav.navigate(tab.route) {
                                                                popUpTo(nav.graph.findStartDestination().id) {
                                                                    saveState = true
                                                                }
                                                                launchSingleTop = true
                                                                restoreState = true
                                                            }
                                                        },
                                                        icon = {
                                                            if (tab.route == "timeline") {
                                                                Box(
                                                                    Modifier
                                                                        .size(26.dp)
                                                                        .clip(CircleShape)
                                                                        .background(WuColors.SurfaceAlt),
                                                                    contentAlignment = Alignment.Center,
                                                                ) {
                                                                    Icon(
                                                                        tab.icon,
                                                                        contentDescription = tab.label,
                                                                        modifier = Modifier.size(16.dp),
                                                                    )
                                                                }
                                                            } else {
                                                                Icon(tab.icon, contentDescription = tab.label)
                                                            }
                                                        },
                                                        label = {
                                                            Text(
                                                                tab.label,
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                overflow = TextOverflow.Clip,
                                                                fontSize = 10.sp,
                                                            )
                                                        },
                                                        alwaysShowLabel = false,
                                                        colors = NavigationBarItemDefaults.colors(
                                                            selectedIconColor = Color.White,
                                                            selectedTextColor = Color.White,
                                                            unselectedIconColor = WuColors.Muted,
                                                            unselectedTextColor = WuColors.Muted,
                                                            indicatorColor = WuColors.NavActive,
                                                        ),
                                                    )
                                                }
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
                                            onOpenSchedule = { nav.navigate("schedule") },
                                            onOpenAccount = { nav.navigate("account") },
                                            onOpenPremium = { nav.navigate("premium") },
                                            onOpenHistory = {
                                                nav.navigate("history") {
                                                    popUpTo(nav.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            onContinueWatch = { item ->
                                                val i = Intent(activity, PlayerActivity::class.java).apply {
                                                    putExtra(PlayerActivity.EXTRA_SLUG, item.slug)
                                                    putEpisodeExtra(
                                                        PlayerActivity.EXTRA_EPISODE,
                                                        item.episode ?: 1.0,
                                                    )
                                                    putExtra(PlayerActivity.EXTRA_TITLE, item.title)
                                                    putExtra(
                                                        PlayerActivity.EXTRA_THUMBNAIL,
                                                        item.thumbnail,
                                                    )
                                                }
                                                activity.startActivity(i)
                                            },
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
                                            onContinue = { item ->
                                                val i = Intent(activity, PlayerActivity::class.java).apply {
                                                    putExtra(PlayerActivity.EXTRA_SLUG, item.slug)
                                                    putEpisodeExtra(
                                                        PlayerActivity.EXTRA_EPISODE,
                                                        item.episode ?: 1.0,
                                                    )
                                                    putExtra(PlayerActivity.EXTRA_TITLE, item.title)
                                                    putExtra(
                                                        PlayerActivity.EXTRA_THUMBNAIL,
                                                        item.thumbnail,
                                                    )
                                                }
                                                activity.startActivity(i)
                                            },
                                        )
                                    }
                                    composable("subscribed") {
                                        SubscribedScreen(
                                            onOpenAnime = { slug -> nav.navigate("detail/$slug") },
                                        )
                                    }
                                    composable("timeline") {
                                        TimelineScreen(
                                            onOpenAccount = { nav.navigate("account") },
                                        )
                                    }
                                    composable("account") {
                                        AccountScreen(
                                            onLogout = {
                                                app.nowPlaying.clear()
                                                loggedIn = false
                                            },
                                            onOpenPremium = { nav.navigate("premium") },
                                        )
                                    }
                                    composable("premium") {
                                        PremiumPackageScreen(
                                            onBack = { nav.popBackStack() },
                                        )
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
                                            onOpenPremium = {
                                                nav.navigate("premium") {
                                                    launchSingleTop = true
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

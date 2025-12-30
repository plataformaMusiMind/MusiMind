package com.musimind

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musimind.data.repository.SubscriptionRepository
import com.musimind.domain.auth.AuthManager
import com.musimind.navigation.MusiMindNavGraph
import com.musimind.navigation.Screen
import com.musimind.ui.theme.MusiMindTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    @Inject
    lateinit var authManager: AuthManager
    
    @Inject
    lateinit var subscriptionRepository: SubscriptionRepository
    
    private var pendingDeepLink: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle deep link from initial launch
        handleDeepLink(intent)
        
        setContent {
            MusiMindTheme {
                MainApp(
                    authManager = authManager,
                    pendingDeepLink = pendingDeepLink,
                    onDeepLinkHandled = { pendingDeepLink = null }
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        val scheme = data.scheme
        val host = data.host
        val path = data.path
        
        if (scheme == "musimind") {
            when (host) {
                "auth" -> {
                    // OAuth callback - Supabase Auth handles this automatically
                    // The session will be refreshed and user will be redirected
                    android.util.Log.d("MainActivity", "OAuth callback received: $data")
                }
                "subscription" -> {
                    when (path) {
                        "/success" -> {
                            // Refresh subscription state
                            CoroutineScope(Dispatchers.Main).launch {
                                subscriptionRepository.refreshSubscriptionState()
                                Toast.makeText(
                                    this@MainActivity,
                                    "🎉 Assinatura ativada com sucesso!",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            pendingDeepLink = "subscription_success"
                        }
                        "/cancel" -> {
                            Toast.makeText(
                                this,
                                "Assinatura cancelada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                "quiz" -> {
                    val quizId = data.lastPathSegment
                    if (quizId != null) {
                        pendingDeepLink = "quiz/$quizId"
                    }
                }
                "duel" -> {
                    val duelId = data.lastPathSegment
                    if (duelId != null) {
                        pendingDeepLink = "duel/$duelId"
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    authManager: AuthManager,
    pendingDeepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Handle deep link navigation
    LaunchedEffect(pendingDeepLink) {
        pendingDeepLink?.let { link ->
            when {
                link == "subscription_success" -> {
                    // Navigate to profile to show updated subscription
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
                link.startsWith("quiz/") -> {
                    val quizId = link.substringAfter("quiz/")
                    navController.navigate(Screen.QuizMultiplayer.createRoute(quizId))
                }
                link.startsWith("duel/") -> {
                    val duelId = link.substringAfter("duel/")
                    navController.navigate(Screen.Duel.createRoute(duelId))
                }
            }
            onDeepLinkHandled()
        }
    }
    
    // Determine if we should show bottom navigation
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Practice.route,
        Screen.Challenges.route,
        Screen.Social.route,
        Screen.Profile.route
    )
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
                if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_home)) },
                        selected = currentRoute == Screen.Home.route,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_practice)) },
                        selected = currentRoute == Screen.Practice.route,
                        onClick = {
                            navController.navigate(Screen.Practice.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_challenges)) },
                        selected = currentRoute == Screen.Challenges.route,
                        onClick = {
                            navController.navigate(Screen.Challenges.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Group, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_social)) },
                        selected = currentRoute == Screen.Social.route,
                        onClick = {
                            navController.navigate(Screen.Social.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text(stringResource(R.string.nav_profile)) },
                        selected = currentRoute == Screen.Profile.route,
                        onClick = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        MusiMindNavGraph(
            navController = navController,
            authManager = authManager,
            modifier = Modifier.padding(innerPadding)
        )
    }
}


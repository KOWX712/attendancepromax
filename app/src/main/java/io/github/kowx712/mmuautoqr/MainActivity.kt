package io.github.kowx712.mmuautoqr

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import io.github.kowx712.mmuautoqr.navigation.homeDestination
import io.github.kowx712.mmuautoqr.navigation.usersDestination
import io.github.kowx712.mmuautoqr.ui.theme.AutoqrTheme
import io.github.kowx712.mmuautoqr.utils.UserManager
import io.github.kowx712.mmuautoqr.viewmodel.HomeViewModelFactory
import io.github.kowx712.mmuautoqr.viewmodel.UserViewModelFactory
import kotlinx.coroutines.launch

sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector
) {
    object Home : BottomNavItem("home", R.string.bottom_nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    object Users : BottomNavItem("users", R.string.bottom_nav_users, Icons.Filled.People, Icons.Outlined.People)
}

val bottomNavItems: List<BottomNavItem> = listOf(
    BottomNavItem.Home,
    BottomNavItem.Users
)

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        val userManager = UserManager(this)

        val homeViewModelFactory = HomeViewModelFactory(userManager)
        val userViewModelFactory = UserViewModelFactory(userManager)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            if (userManager.getActiveUserCount() > 0) {
                startActivity(Intent(this@MainActivity, QRScannerActivity::class.java))
            }
        }

        setContent {
            AutoqrTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                Scaffold(
                    bottomBar = {
                        BottomNav(navController = navController)
                    }
                ) { innerPadding ->
                    val duration = 300
                    NavHost(
                        navController = navController,
                        startDestination = BottomNavItem.Home.route,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = { fadeIn(animationSpec = tween(duration)) },
                        exitTransition = { fadeOut(animationSpec = tween(duration)) },
                    ) {
                        homeDestination(
                            activity = this@MainActivity,
                            navController = navController,
                            homeViewModelFactory = homeViewModelFactory
                        )
                        usersDestination(
                            context = context,
                            userViewModelFactory = userViewModelFactory
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNav(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) screen.filledIcon else screen.outlinedIcon,
                        contentDescription = stringResource(screen.titleResId)
                    )
                },
                label = { Text(stringResource(screen.titleResId)) },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
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

package io.github.kowx712.mmuautoqr

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import io.github.kowx712.mmuautoqr.ui.screens.MainScreen
import io.github.kowx712.mmuautoqr.ui.screens.UserScreen
import io.github.kowx712.mmuautoqr.ui.theme.AutoqrTheme
import io.github.kowx712.mmuautoqr.utils.UserManager
import io.github.kowx712.mmuautoqr.viewmodels.HomeViewModel
import io.github.kowx712.mmuautoqr.viewmodels.HomeViewModelFactory
import io.github.kowx712.mmuautoqr.viewmodels.UserOperationFeedback
import io.github.kowx712.mmuautoqr.viewmodels.UserViewModel
import io.github.kowx712.mmuautoqr.viewmodels.UserViewModelFactory
import kotlinx.coroutines.flow.collectLatest
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

val bottomNavItems = listOf(
    BottomNavItem.Home,
    BottomNavItem.Users
)

class MainActivity : ComponentActivity() {
    private lateinit var userManager: UserManager

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
        }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        userManager = UserManager(this)

        val homeViewModelFactory = HomeViewModelFactory(userManager)
        val userViewModelFactory = UserViewModelFactory(userManager)
        val hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)

        if (!hasCameraPermission) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        lifecycleScope.launch {
            if (userManager.getActiveUserCount() > 0) {
                startActivity(Intent(this@MainActivity, QRScannerActivity::class.java))
            }
        }

        setContent {
            AutoqrTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val context = LocalContext.current
                val activity = context as? Activity

                Scaffold(
                    contentWindowInsets = WindowInsets.systemBars,
                    bottomBar = {
                        NavigationBar {
                            val currentDestination = navBackStackEntry?.destination
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
                ) { innerPadding ->
                    val duration = 300
                    val currentRoute = navBackStackEntry?.destination?.route
                    if (currentRoute == BottomNavItem.Home.route || currentRoute == BottomNavItem.Users.route) {
                        BackHandler(enabled = true) {
                            activity?.finishAffinity()
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = BottomNavItem.Home.route,
                        modifier = Modifier.padding(innerPadding),
                        enterTransition = { fadeIn(animationSpec = tween(duration)) },
                        exitTransition = { fadeOut(animationSpec = tween(duration)) },
                        popEnterTransition = { fadeIn(animationSpec = tween(duration)) },
                        popExitTransition = { fadeOut(animationSpec = tween(duration)) }
                    ) {
                        composable(BottomNavItem.Home.route) {
                            val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
                            val totalUsers by homeViewModel.totalUsers
                            val activeUsers by homeViewModel.activeUsers

                            LaunchedEffect(Unit) {
                                homeViewModel.refreshStats()
                            }

                            MainScreen(
                                totalUsers = totalUsers,
                                activeUsers = activeUsers,
                                onRefreshStats = {
                                    homeViewModel.refreshStats()
                                },
                                onScanQr = {
                                    if (activeUsers > 0) {
                                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                            startActivity(Intent(this@MainActivity, QRScannerActivity::class.java))
                                        } else {
                                            Toast.makeText(this@MainActivity, getString(R.string.camera_permission_needed), Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(this@MainActivity, getString(R.string.add_users_first), Toast.LENGTH_LONG).show()
                                        navController.navigate(BottomNavItem.Users.route)
                                    }
                                }
                            )
                        }
                        composable(BottomNavItem.Users.route) {
                            val userViewModel: UserViewModel = viewModel(factory = userViewModelFactory)
                            val usersList by userViewModel.usersList

                            LaunchedEffect(Unit) {
                                userViewModel.operationFeedback.collectLatest { feedback ->
                                    val message = when (feedback) {
                                        is UserOperationFeedback.Success -> getString(feedback.messageResId)
                                        is UserOperationFeedback.Error -> getString(feedback.messageResId)
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }

                            UserScreen(
                                users = usersList,
                                onAddUser = { name, userId, password ->
                                    userViewModel.addUser(name, userId, password)
                                },
                                onUpdateUser = { user, name, password ->
                                    userViewModel.updateUser(user, name, password)
                                },
                                onDeleteUser = { user ->
                                    userViewModel.deleteUser(user)
                                },
                                onToggleUserStatus = { userToToggle ->
                                    userViewModel.toggleUserStatus(userToToggle)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }
}

package io.github.kowx712.mmuautoqr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import io.github.kowx712.mmuautoqr.ui.navigation3.LocalNavigator
import io.github.kowx712.mmuautoqr.ui.navigation3.Navigator
import io.github.kowx712.mmuautoqr.ui.navigation3.Route
import io.github.kowx712.mmuautoqr.ui.navigation3.rememberNavigator
import io.github.kowx712.mmuautoqr.ui.screens.MainScreen
import io.github.kowx712.mmuautoqr.ui.screens.SettingsScreen
import io.github.kowx712.mmuautoqr.ui.screens.UserScreen
import io.github.kowx712.mmuautoqr.ui.theme.AutoqrTheme
import io.github.kowx712.mmuautoqr.utils.AppSettings
import io.github.kowx712.mmuautoqr.utils.UserManager
import io.github.kowx712.mmuautoqr.utils.WebViewHtmlSnapshotStore
import io.github.kowx712.mmuautoqr.viewmodel.HomeViewModel
import io.github.kowx712.mmuautoqr.viewmodel.HomeViewModelFactory
import io.github.kowx712.mmuautoqr.viewmodel.SettingsViewModel
import io.github.kowx712.mmuautoqr.viewmodel.SettingsViewModelFactory
import io.github.kowx712.mmuautoqr.viewmodel.UserOperationFeedback
import io.github.kowx712.mmuautoqr.viewmodel.UserViewModel
import io.github.kowx712.mmuautoqr.viewmodel.UserViewModelFactory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val SETTINGS_EXPORT_LOG_TAG = "SettingsExport"

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        val userManager = UserManager(this)
        val appSettings = AppSettings(this)

        val homeViewModelFactory = HomeViewModelFactory(userManager)
        val userViewModelFactory = UserViewModelFactory(userManager)
        val settingsViewModelFactory = SettingsViewModelFactory(appSettings)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        actionBar?.hide()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            if (appSettings.isOpenQrScannerAutomaticallyEnabled() && userManager.getActiveUserCount() > 0) {
                startActivity(Intent(this@MainActivity, QRScannerActivity::class.java))
            }
        }

        setContent {
            AutoqrTheme {
                val navigator = rememberNavigator<Route>(startRoute = Route.Home)

                CompositionLocalProvider(LocalNavigator provides navigator) {
                    Scaffold(
                        bottomBar = {
                            BottomBar()
                        }
                    ) { paddingValues ->
                        val myEntryProvider: (Route) -> NavEntry<Route> = { key ->
                            when (key) {
                                is Route.Home -> NavEntry<Route>(key) {
                                    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)
                                    val totalUsers by homeViewModel.totalUsers
                                    val activeUsers by homeViewModel.activeUsers
                                    val cameraPermissionMsg = stringResource(R.string.camera_permission_needed)
                                    val addUsersFirstMsg = stringResource(R.string.add_users_first)

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
                                                    this@MainActivity.startActivity(Intent(this@MainActivity, QRScannerActivity::class.java))
                                                } else {
                                                    Toast.makeText(this@MainActivity, cameraPermissionMsg, Toast.LENGTH_LONG).show()
                                                }
                                            } else {
                                                Toast.makeText(this@MainActivity, addUsersFirstMsg, Toast.LENGTH_LONG).show()
                                                navigator.push(Route.Users)
                                            }
                                        }
                                    )
                                }
                                is Route.Users -> NavEntry(key) {
                                    val userViewModel: UserViewModel = viewModel(factory = userViewModelFactory)
                                    val usersList by userViewModel.usersList

                                    LaunchedEffect(Unit) {
                                        userViewModel.operationFeedback.collectLatest { feedback ->
                                            val message = when (feedback) {
                                                is UserOperationFeedback.Success -> this@MainActivity.getString(feedback.messageResId)
                                                is UserOperationFeedback.Error -> this@MainActivity.getString(feedback.messageResId)
                                            }
                                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
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
                                is Route.Settings -> NavEntry(key) {
                                    val settingsViewModel: SettingsViewModel =
                                        viewModel(factory = settingsViewModelFactory)
                                    val isOpenQrScannerAutomaticallyEnabled by
                                        settingsViewModel.isOpenQrScannerAutomaticallyEnabled
                                    val htmlSnapshotStore = remember {
                                        WebViewHtmlSnapshotStore(
                                            snapshotDir = File(cacheDir, "webview-snapshots"),
                                            exportDir = File(cacheDir, "webview-snapshot-exports")
                                        )
                                    }
                                    val noRenderedHtmlLogsMessage =
                                        stringResource(R.string.no_rendered_html_logs)
                                    val exportRenderedHtmlLogsSucceededMessage =
                                        stringResource(R.string.export_rendered_html_logs_succeeded)
                                    val exportRenderedHtmlLogsFailedMessage =
                                        stringResource(R.string.export_rendered_html_logs_failed)
                                    var pendingExportZip by remember { mutableStateOf<File?>(null) }
                                    val exportLauncher =
                                        rememberLauncherForActivityResult(
                                            contract = ActivityResultContracts.CreateDocument("application/zip")
                                        ) { destinationUri ->
                                            val tempZip = pendingExportZip
                                            pendingExportZip = null

                                            if (destinationUri == null || tempZip == null) {
                                                tempZip?.let(htmlSnapshotStore::deleteTempZip)
                                                return@rememberLauncherForActivityResult
                                            }

                                            lifecycleScope.launch {
                                                val wasSuccessful = runCatching {
                                                    withContext(Dispatchers.IO) {
                                                        contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                                                            htmlSnapshotStore.copyZipTo(outputStream, tempZip)
                                                        } ?: error("Failed to open export destination")
                                                    }
                                                }.onFailure { error ->
                                                    Log.w(SETTINGS_EXPORT_LOG_TAG, "Failed to export rendered HTML logs", error)
                                                }.isSuccess

                                                withContext(Dispatchers.IO) {
                                                    htmlSnapshotStore.deleteTempZip(tempZip)
                                                }
                                                Toast.makeText(
                                                    this@MainActivity,
                                                    if (wasSuccessful) {
                                                        exportRenderedHtmlLogsSucceededMessage
                                                    } else {
                                                        exportRenderedHtmlLogsFailedMessage
                                                    },
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }

                                    SettingsScreen(
                                        isOpenQrScannerAutomaticallyEnabled = isOpenQrScannerAutomaticallyEnabled,
                                        onOpenQrScannerAutomaticallyChanged = { enabled ->
                                            settingsViewModel.setOpenQrScannerAutomaticallyEnabled(enabled)
                                        },
                                        onExportRenderedHtmlLogs = {
                                            lifecycleScope.launch {
                                                val exportResult = withContext(Dispatchers.IO) {
                                                    runCatching {
                                                        htmlSnapshotStore.createExportZip()
                                                    }
                                                }
                                                val exportZip = exportResult.getOrNull()

                                                if (exportResult.isFailure) {
                                                    Log.w(
                                                        SETTINGS_EXPORT_LOG_TAG,
                                                        "Failed to create rendered HTML log export zip",
                                                        exportResult.exceptionOrNull()
                                                    )
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        exportRenderedHtmlLogsFailedMessage,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else if (exportZip == null) {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        noRenderedHtmlLogsMessage,
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    pendingExportZip = exportZip
                                                    exportLauncher.launch(exportZip.name)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        val enter = fadeIn(animationSpec = tween(240))
                        val exit = fadeOut(animationSpec = tween(240))

                        NavDisplay(
                            backStack = navigator.backStack,
                            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding()),
                            entryProvider = myEntryProvider,
                            transitionSpec = { enter togetherWith exit },
                            popTransitionSpec = { enter togetherWith exit },
                            predictivePopTransitionSpec = { enter togetherWith exit }
                        )
                    }
                }
            }
        }
    }
}

val navItems: List<Route> = listOf(
    Route.Home,
    Route.Users,
    Route.Settings,
)

@Composable
private fun BottomBar() {
    @Suppress("UNCHECKED_CAST")
    val navigator = LocalNavigator.current as Navigator<Route>
    val currentKey = navigator.current()

    NavigationBar {
        navItems.forEach { screen ->
            val selected = currentKey == screen
            NavigationBarItem(
                icon = {
                    Icon(
                        if (selected) screen.filledIcon else screen.outlinedIcon,
                        contentDescription = stringResource(screen.titleResId)
                    )
                },
                label = { Text(stringResource(screen.titleResId)) },
                selected = selected,
                onClick = {
                    if (currentKey != screen) {
                        navigator.popUntil { it == navItems.first() }
                        if (screen != navItems.first()) {
                            navigator.push(screen)
                        }
                    }
                }
            )
        }
    }
}

package io.github.kowx712.mmuautoqr

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import io.github.kowx712.mmuautoqr.ui.navigation3.LocalNavigator
import io.github.kowx712.mmuautoqr.ui.navigation3.Route
import io.github.kowx712.mmuautoqr.ui.navigation3.rememberNavigator
import io.github.kowx712.mmuautoqr.ui.screens.HomeScreen
import io.github.kowx712.mmuautoqr.ui.screens.SettingsScreen
import io.github.kowx712.mmuautoqr.ui.screens.UserScreen
import io.github.kowx712.mmuautoqr.ui.theme.AutoqrTheme
import io.github.kowx712.mmuautoqr.utils.AppSettings
import io.github.kowx712.mmuautoqr.utils.UserManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        val userManager = UserManager(this)
        val appSettings = AppSettings(this)

        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        actionBar?.hide()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            if (appSettings.isOpenQrScannerEnabled() && userManager.getActiveUserCount() > 0) {
                startActivity(Intent(this@MainActivity, QRScannerActivity::class.java))
            }
        }

        setContent {
            AutoqrTheme {
                val navigator = rememberNavigator<Route>(startRoute = Route.Main)

                CompositionLocalProvider(LocalNavigator provides navigator) {
                    NavDisplay(
                        backStack = navigator.backStack,
                        entryProvider = entryProvider {
                            entry<Route.Main> { MainScreenEntry() }
                        },
                        transitionSpec = {
                            val enter = slideInHorizontally(initialOffsetX = { it })
                            val exit = slideOutHorizontally(targetOffsetX = { -it / 4 }) + fadeOut()
                            enter togetherWith exit
                        },
                        popTransitionSpec = {
                            val enter = slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
                            val exit = scaleOut(targetScale = 0.9f) + fadeOut()
                            enter togetherWith exit
                        },
                        predictivePopTransitionSpec = {
                            val enter = slideInHorizontally(initialOffsetX = { -it / 4 }) + fadeIn()
                            val exit = scaleOut(targetScale = 0.9f) + fadeOut()
                            enter togetherWith exit
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreenEntry() {
    val controller = rememberMainPageController()
    val currentPage = controller.currentPage

    BackHandler(enabled = currentPage != MainPage.Home) {
        controller.handleBack()
    }

    CompositionLocalProvider(
        LocalMainPageController provides controller
    ) {
        Scaffold(
            bottomBar = {
                BottomBar(
                    currentPage = currentPage,
                    onPageSelected = controller::navigateTo
                )
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    fadeIn(tween(340)) togetherWith fadeOut(tween(340))
                },
                label = "MainPageAnimatedContent"
            ) { page ->
                val bottomInnerPadding = innerPadding.calculateBottomPadding()
                when (page) {
                    MainPage.Home -> HomeScreen(bottomInnerPadding)
                    MainPage.Users -> UserScreen(bottomInnerPadding)
                    MainPage.Settings -> SettingsScreen(bottomInnerPadding)
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    currentPage: MainPage,
    onPageSelected: (MainPage) -> Unit,
) {
    NavigationBar {
        MainPage.entries.forEach { page ->
            val selected = currentPage == page
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selected) page.filledIcon else page.outlinedIcon,
                        contentDescription = stringResource(page.titleResId)
                    )
                },
                label = { Text(stringResource(page.titleResId)) },
                selected = selected,
                onClick = {
                    if (!selected) {
                        onPageSelected(page)
                    }
                }
            )
        }
    }
}

@Composable
private fun rememberMainPageController(
    initialPage: MainPage = MainPage.Home,
): MainPageController {
    return rememberSaveable(saver = MainPageController.Saver) {
        MainPageController(initialPage)
    }
}

@Immutable
enum class MainPage(
    val titleResId: Int,
    val filledIcon: ImageVector,
    val outlinedIcon: ImageVector,
) {
    Home(
        titleResId = R.string.bottom_nav_home,
        filledIcon = Icons.Filled.Home,
        outlinedIcon = Icons.Outlined.Home
    ),
    Users(
        titleResId = R.string.bottom_nav_users,
        filledIcon = Icons.Filled.People,
        outlinedIcon = Icons.Outlined.People
    ),
    Settings(
        titleResId = R.string.bottom_nav_settings,
        filledIcon = Icons.Filled.Settings,
        outlinedIcon = Icons.Outlined.Settings
    )
}

@Stable
class MainPageController(
    initialPage: MainPage = MainPage.Home,
) {
    private val currentPageState = mutableStateOf(initialPage)

    val currentPage: MainPage
        get() = currentPageState.value

    fun navigateTo(page: MainPage) {
        currentPageState.value = page
    }

    fun handleBack(): Boolean {
        if (currentPage == MainPage.Home) {
            return false
        }
        currentPageState.value = MainPage.Home
        return true
    }

    companion object {
        val Saver: Saver<MainPageController, String> = Saver(
            save = { it.currentPage.name },
            restore = { savedPageName -> MainPageController(MainPage.valueOf(savedPageName)) }
        )
    }
}

val LocalMainPageController = staticCompositionLocalOf<MainPageController?> { null }

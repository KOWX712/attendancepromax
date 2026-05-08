package io.github.kowx712.mmuautoqr.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kowx712.mmuautoqr.LocalMainPageController
import io.github.kowx712.mmuautoqr.MainPage
import io.github.kowx712.mmuautoqr.QRScannerActivity
import io.github.kowx712.mmuautoqr.R
import io.github.kowx712.mmuautoqr.utils.UserManager
import io.github.kowx712.mmuautoqr.viewmodel.UserViewModel
import io.github.kowx712.mmuautoqr.viewmodel.UserViewModelFactory

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreenContent(
        totalUsers = 2,
        activeUsers = 2,
        onRefreshStats = {},
        onScanQr = {},
        bottomInnerPadding = 0.dp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    bottomInnerPadding: Dp,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val owner = context as? ViewModelStoreOwner ?: error("HomeScreen requires a ViewModelStoreOwner context")
    val viewModel: UserViewModel = viewModel(
        viewModelStoreOwner = owner,
        key = "user_view_model",
        factory = remember(appContext) {
            UserViewModelFactory(UserManager(appContext))
        }
    )
    val mainPageController = LocalMainPageController.current
    val totalUsers by viewModel.totalUsers
    val activeUsers by viewModel.activeUsers
    val cameraPermissionMsg = stringResource(R.string.camera_permission_needed)
    val addUsersFirstMsg = stringResource(R.string.add_users_first)

    LaunchedEffect(mainPageController?.currentPage) {
        if (mainPageController?.currentPage == MainPage.Home) {
            viewModel.refreshStats()
        }
    }

    HomeScreenContent(
        totalUsers = totalUsers,
        activeUsers = activeUsers,
        onRefreshStats = viewModel::refreshStats,
        onScanQr = {
            if (activeUsers > 0) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    context.startActivity(Intent(context, QRScannerActivity::class.java))
                } else {
                    Toast.makeText(context, cameraPermissionMsg, Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, addUsersFirstMsg, Toast.LENGTH_LONG).show()
                mainPageController?.navigateTo(MainPage.Users)
            }
        },
        bottomInnerPadding = bottomInnerPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    totalUsers: Int,
    activeUsers: Int,
    onRefreshStats: () -> Unit,
    onScanQr: () -> Unit,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.main_screen_title)) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (activeUsers > 0) {
                FloatingActionButton(
                    modifier = Modifier.padding(bottom = bottomInnerPadding),
                    onClick = {
                        onScanQr()
                        onRefreshStats()
                    }
                ) {
                    Icon(Icons.Default.QrCode, stringResource(R.string.scan_qr_code))
                }
            }
        },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = stringResource(R.string.user_statistics),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.total_users, totalUsers),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            text = stringResource(R.string.active_users, activeUsers),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    Text(
                        text = stringResource(R.string.main_screen_instructions_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            item {
                Spacer(Modifier.height(bottomInnerPadding))
            }
        }
    }
}

package io.github.kowx712.mmuautoqr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.kowx712.mmuautoqr.ui.theme.AutoqrTheme
import io.github.kowx712.mmuautoqr.utils.UserManager

class MainActivity : ComponentActivity() {
    private lateinit var userManager: UserManager

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Toast.makeText(this, getString(R.string.camera_permission_granted), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userManager = UserManager(this)

        val currentActiveUserCount = userManager.activeUserCount
        val cameraPermissionAlreadyGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        if (currentActiveUserCount > 0 && cameraPermissionAlreadyGranted) {
            startActivity(Intent(this, QRScannerActivity::class.java))
        }

        setContent {
            val darkTheme = isSystemInDarkTheme()
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT
                ) { !darkTheme }
            )
            AutoqrTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val dark = isSystemInDarkTheme()
                    SideEffect {
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        controller.isAppearanceLightStatusBars = !dark
                        controller.isAppearanceLightNavigationBars = !dark
                    }
                    var totalUsers by remember { mutableStateOf(0) }
                    var activeUsers by remember { mutableStateOf(0) }

                    LaunchedEffect(Unit) {
                        totalUsers = userManager.userCount
                        activeUsers = userManager.activeUserCount
                        if (!cameraPermissionAlreadyGranted) {
                            checkCameraPermission()
                        }
                    }

                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                totalUsers = userManager.userCount
                                activeUsers = userManager.activeUserCount
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }

                    MainScreen(
                        totalUsers = totalUsers,
                        activeUsers = activeUsers,
                        onRefreshStats = {
                            totalUsers = userManager.userCount
                            activeUsers = userManager.activeUserCount
                        },
                        onScanQr = {
                            if (userManager.activeUserCount > 0) {
                                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    startActivity(Intent(this@MainActivity, QRScannerActivity::class.java))
                                } else {
                                    Toast.makeText(this@MainActivity, getString(R.string.camera_permission_needed), Toast.LENGTH_LONG).show()
                                    checkCameraPermission()
                                }
                            } else {
                                // This case should ideally not be hit if button is disabled.
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.add_users_first),
                                    Toast.LENGTH_LONG
                                ).show()
                                startActivity(Intent(this@MainActivity, UserManagementActivity::class.java))
                            }
                        },
                        onManageUsers = {
                            startActivity(Intent(this@MainActivity, UserManagementActivity::class.java))
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun checkCameraPermission() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }
}

@Composable
private fun MainScreen(
    totalUsers: Int,
    activeUsers: Int,
    onRefreshStats: () -> Unit,
    onScanQr: () -> Unit,
    onManageUsers: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 26.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.main_screen_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.app_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 28.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = stringResource(R.string.user_statistics),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.total_users, totalUsers),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    text = stringResource(R.string.active_users, activeUsers),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Button(
            onClick = { onScanQr(); onRefreshStats() },
            enabled = activeUsers > 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp)
        ) {
            Text(text = stringResource(R.string.scan_qr_code))
        }

        Button(
            onClick = { onManageUsers() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 28.dp)
        ) {
            Text(text = stringResource(R.string.manage_users))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Text(
                text = stringResource(R.string.main_screen_instructions_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

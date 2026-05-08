package io.github.kowx712.mmuautoqr.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kowx712.mmuautoqr.R
import io.github.kowx712.mmuautoqr.ui.theme.AutoqrTheme
import io.github.kowx712.mmuautoqr.utils.AppSettings
import io.github.kowx712.mmuautoqr.utils.WebViewHtmlSnapshotStore
import io.github.kowx712.mmuautoqr.viewmodel.RenderedHtmlLogExporter
import io.github.kowx712.mmuautoqr.viewmodel.SettingsExportResult
import io.github.kowx712.mmuautoqr.viewmodel.SettingsViewModel
import io.github.kowx712.mmuautoqr.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    AutoqrTheme {
        SettingsScreenContent(
            isOpenQrScannerEnabled = true,
            onOpenQrScannerChanged = {},
            onExportRenderedHtmlLogs = {},
            bottomInnerPadding = 0.dp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bottomInnerPadding: Dp,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val exporter = remember {
        SnapshotStoreRenderedHtmlLogExporter(
            WebViewHtmlSnapshotStore(
                snapshotDir = File(appContext.cacheDir, "webview-snapshots"),
                exportDir = File(appContext.cacheDir, "webview-snapshot-exports")
            )
        )
    }
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(
            appSettings = AppSettings(appContext),
            renderedHtmlLogExporter = exporter
        )
    )
    val isOpenQrScannerEnabled by viewModel.isOpenQrScannerEnabled
    val noRenderedHtmlLogsMessage = stringResource(R.string.no_rendered_html_logs)
    val exportRenderedHtmlLogsSucceededMessage = stringResource(R.string.export_rendered_html_logs_succeeded)
    val exportRenderedHtmlLogsFailedMessage = stringResource(R.string.export_rendered_html_logs_failed)
    var pendingExportZip by remember { mutableStateOf<File?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { destinationUri ->
        val tempZip = pendingExportZip
        pendingExportZip = null

        if (destinationUri == null || tempZip == null) {
            tempZip?.let(viewModel::deleteTempZip)
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val wasSuccessful = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                        exporter.copyZipTo(outputStream, tempZip)
                    } ?: error("Failed to open export destination")
                }
            }.onFailure { error ->
                Log.w(SETTINGS_EXPORT_LOG_TAG, "Failed to export rendered HTML logs", error)
            }.isSuccess

            viewModel.deleteTempZip(tempZip)
            Toast.makeText(
                context,
                if (wasSuccessful) {
                    exportRenderedHtmlLogsSucceededMessage
                } else {
                    exportRenderedHtmlLogsFailedMessage
                },
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    SettingsScreenContent(
        isOpenQrScannerEnabled = isOpenQrScannerEnabled,
        onOpenQrScannerChanged = viewModel::setOpenQrScannerAutomaticallyEnabled,
        onExportRenderedHtmlLogs = {
            exportRenderedHtmlLogs(
                scope = scope,
                viewModel = viewModel,
                onNoLogs = {
                    Toast.makeText(context, noRenderedHtmlLogsMessage, Toast.LENGTH_SHORT).show()
                },
                onFailure = { error ->
                    Log.w(
                        SETTINGS_EXPORT_LOG_TAG,
                        "Failed to create rendered HTML log export zip",
                        error
                    )
                    Toast.makeText(
                        context,
                        exportRenderedHtmlLogsFailedMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onReady = { exportZip ->
                    pendingExportZip = exportZip
                    exportLauncher.launch(exportZip.name)
                }
            )
        },
        bottomInnerPadding = bottomInnerPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    isOpenQrScannerEnabled: Boolean,
    onOpenQrScannerChanged: (Boolean) -> Unit,
    onExportRenderedHtmlLogs: () -> Unit,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            item {
                SwitchItem(
                    headlineContent = { Text(stringResource(R.string.open_qr_scanner)) },
                    leadingContent = { Icon(Icons.Rounded.QrCode, null) },
                    supportingContent = { Text(stringResource(R.string.open_qr_scanner_summary)) },
                    checked = isOpenQrScannerEnabled,
                    onCheckedChange = onOpenQrScannerChanged,
                )
                ListItem(
                    modifier = Modifier.clickable { onExportRenderedHtmlLogs() },
                    leadingContent = { Icon(Icons.Rounded.Save, null) },
                    headlineContent = { Text(stringResource(R.string.export_logs)) },
                    supportingContent = { Text(stringResource(R.string.export_logs_summary)) },
                )
            }
            item {
                Spacer(Modifier.height(bottomInnerPadding))
            }
        }
    }
}

private fun exportRenderedHtmlLogs(
    scope: CoroutineScope,
    viewModel: SettingsViewModel,
    onNoLogs: () -> Unit,
    onFailure: (Throwable) -> Unit,
    onReady: (File) -> Unit,
) {
    scope.launch {
        when (val result = viewModel.prepareRenderedHtmlLogsExport()) {
            is SettingsExportResult.NoLogs -> onNoLogs()
            is SettingsExportResult.Failure -> onFailure(result.error)
            is SettingsExportResult.Ready -> onReady(result.zipFile)
        }
    }
}

private class SnapshotStoreRenderedHtmlLogExporter(
    private val snapshotStore: WebViewHtmlSnapshotStore,
) : RenderedHtmlLogExporter {
    override fun createExportZip(): File? = snapshotStore.createExportZip()

    override fun deleteTempZip(zipFile: File) {
        snapshotStore.deleteTempZip(zipFile)
    }

    fun copyZipTo(outputStream: java.io.OutputStream, zipFile: File) {
        snapshotStore.copyZipTo(outputStream, zipFile)
    }
}

private const val SETTINGS_EXPORT_LOG_TAG = "SettingsExport"

@Composable
fun SwitchItem(
    modifier: Modifier = Modifier,
    leadingContent: @Composable (() -> Unit),
    headlineContent: @Composable (() -> Unit),
    supportingContent: @Composable (() -> Unit),
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = modifier.clickable { onCheckedChange(!checked) },
        leadingContent = leadingContent,
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    )
}

package io.github.kowx712.mmuautoqr.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.kowx712.mmuautoqr.R
import io.github.kowx712.mmuautoqr.ui.theme.AutoqrTheme

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    AutoqrTheme {
        SettingsScreen(
            isOpenQrScannerAutomaticallyEnabled = true,
            onOpenQrScannerAutomaticallyChanged = {},
            onExportRenderedHtmlLogs = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isOpenQrScannerAutomaticallyEnabled: Boolean,
    onOpenQrScannerAutomaticallyChanged: (Boolean) -> Unit,
    onExportRenderedHtmlLogs: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true }
    )

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
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
        ) {
            item {
                SwitchItem(
                    headlineContent = { Text(stringResource(R.string.open_qr_scanner)) },
                    supportingContent = { Text(stringResource(R.string.open_qr_scanner_summary)) },
                    checked = isOpenQrScannerAutomaticallyEnabled,
                    onCheckedChange = onOpenQrScannerAutomaticallyChanged,
                )
                ListItem(
                    modifier = Modifier.clickable { onExportRenderedHtmlLogs() },
                    leadingContent = { Icon(Icons.Rounded.Save, null) },
                    headlineContent = { Text(stringResource(R.string.export_logs)) },
                    supportingContent = { Text(stringResource(R.string.export_logs_summary)) },
                )
            }
        }
    }
}

@Composable
fun SwitchItem(
    modifier: Modifier = Modifier,
    headlineContent: @Composable (() -> Unit),
    supportingContent: @Composable (() -> Unit),
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        modifier = modifier.clickable { onCheckedChange(!checked) },
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

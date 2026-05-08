package io.github.kowx712.mmuautoqr.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.kowx712.mmuautoqr.utils.AppSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface SettingsExportResult {
    data object NoLogs : SettingsExportResult

    data class Ready(val zipFile: File) : SettingsExportResult

    data class Failure(val error: Throwable) : SettingsExportResult
}

interface RenderedHtmlLogExporter {
    fun createExportZip(): File?

    fun deleteTempZip(zipFile: File)
}

class SettingsViewModel(
    private val appSettings: AppSettings,
    private val renderedHtmlLogExporter: RenderedHtmlLogExporter,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _isOpenQrScannerEnabled = mutableStateOf(true)
    val isOpenQrScannerEnabled: State<Boolean> = _isOpenQrScannerEnabled

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isOpenQrScannerEnabled.value = withContext(ioDispatcher) {
                appSettings.isOpenQrScannerEnabled()
            }
        }
    }

    fun setOpenQrScannerAutomaticallyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                appSettings.setOpenQrScannerEnabled(enabled)
            }
            _isOpenQrScannerEnabled.value = enabled
        }
    }

    suspend fun prepareRenderedHtmlLogsExport(): SettingsExportResult =
        withContext(ioDispatcher) {
            runCatching {
                renderedHtmlLogExporter.createExportZip()
            }.fold(
                onSuccess = { exportZip ->
                    if (exportZip == null) {
                        SettingsExportResult.NoLogs
                    } else {
                        SettingsExportResult.Ready(exportZip)
                    }
                },
                onFailure = { error ->
                    SettingsExportResult.Failure(error)
                }
            )
        }

    fun deleteTempZip(zipFile: File) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                renderedHtmlLogExporter.deleteTempZip(zipFile)
            }
        }
    }
}

class SettingsViewModelFactory(
    private val appSettings: AppSettings,
    private val renderedHtmlLogExporter: RenderedHtmlLogExporter,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(appSettings, renderedHtmlLogExporter) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

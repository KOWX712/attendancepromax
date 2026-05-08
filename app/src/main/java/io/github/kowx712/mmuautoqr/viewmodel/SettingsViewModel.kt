package io.github.kowx712.mmuautoqr.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.kowx712.mmuautoqr.utils.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val appSettings: AppSettings,
) : ViewModel() {
    private val _isOpenQrScannerAutomaticallyEnabled = mutableStateOf(true)
    val isOpenQrScannerAutomaticallyEnabled: State<Boolean> = _isOpenQrScannerAutomaticallyEnabled

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isOpenQrScannerAutomaticallyEnabled.value = withContext(Dispatchers.IO) {
                appSettings.isOpenQrScannerAutomaticallyEnabled()
            }
        }
    }

    fun setOpenQrScannerAutomaticallyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                appSettings.setOpenQrScannerAutomaticallyEnabled(enabled)
            }
            _isOpenQrScannerAutomaticallyEnabled.value = enabled
        }
    }
}

class SettingsViewModelFactory(
    private val appSettings: AppSettings,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(appSettings) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

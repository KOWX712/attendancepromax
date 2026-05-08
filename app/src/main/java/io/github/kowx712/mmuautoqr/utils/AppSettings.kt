package io.github.kowx712.mmuautoqr.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

private const val APP_SETTINGS_PREFS_NAME = "app_settings"
private const val AUTO_OPEN_QR_SCANNER_KEY = "auto_open_qr_scanner"

interface BooleanStore {
    fun getBoolean(key: String, defaultValue: Boolean): Boolean

    fun putBoolean(key: String, value: Boolean)
}

class InMemoryBooleanStore : BooleanStore {
    private val values = mutableMapOf<String, Boolean>()

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }
}

private class SharedPreferencesBooleanStore(
    private val sharedPreferences: SharedPreferences,
) : BooleanStore {
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        sharedPreferences.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }
}

class AppSettings(
    private val booleanStore: BooleanStore,
) {
    constructor(context: Context) : this(
        SharedPreferencesBooleanStore(
            context.getSharedPreferences(APP_SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
        )
    )

    fun isOpenQrScannerEnabled(): Boolean =
        booleanStore.getBoolean(AUTO_OPEN_QR_SCANNER_KEY, true)

    fun setOpenQrScannerEnabled(enabled: Boolean) {
        booleanStore.putBoolean(AUTO_OPEN_QR_SCANNER_KEY, enabled)
    }
}

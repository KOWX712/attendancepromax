import 'package:mmuautoqr/app/ui_mode.dart';
import 'package:mmuautoqr/core/storage/key_value_store.dart';

abstract interface class SettingsRepository {
  Future<UiModePreference> getUiModePreference();

  Future<void> setUiModePreference(UiModePreference preference);

  Future<bool> isAutoOpenQrScannerEnabled();

  Future<void> setAutoOpenQrScannerEnabled(bool enabled);
}

class LocalSettingsRepository implements SettingsRepository {
  LocalSettingsRepository(this._store);

  final KeyValueStore _store;

  static const _uiModePreferenceKey = 'settings.ui_mode_preference';
  static const _autoOpenQrScannerKey = 'settings.auto_open_qr_scanner';

  @override
  Future<UiModePreference> getUiModePreference() async {
    final rawValue = await _store.read(_uiModePreferenceKey);
    return UiModePreference.values.firstWhere(
      (value) => value.name == rawValue,
      orElse: () => UiModePreference.system,
    );
  }

  @override
  Future<bool> isAutoOpenQrScannerEnabled() async {
    final rawValue = await _store.read(_autoOpenQrScannerKey);
    return rawValue == null ? true : rawValue == 'true';
  }

  @override
  Future<void> setAutoOpenQrScannerEnabled(bool enabled) {
    return _store.write(_autoOpenQrScannerKey, enabled.toString());
  }

  @override
  Future<void> setUiModePreference(UiModePreference preference) {
    return _store.write(_uiModePreferenceKey, preference.name);
  }
}

import 'package:mmuautoqr/app/ui_mode.dart';

class AppSettingsState {
  const AppSettingsState({
    required this.uiModePreference,
    required this.autoOpenQrScanner,
  });

  final UiModePreference uiModePreference;
  final bool autoOpenQrScanner;

  AppSettingsState copyWith({
    UiModePreference? uiModePreference,
    bool? autoOpenQrScanner,
  }) {
    return AppSettingsState(
      uiModePreference: uiModePreference ?? this.uiModePreference,
      autoOpenQrScanner: autoOpenQrScanner ?? this.autoOpenQrScanner,
    );
  }
}

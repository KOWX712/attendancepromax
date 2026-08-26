enum UiModePreference {
  system,
  apple,
  android,
}

enum EffectiveUiMode {
  apple,
  android,
}

enum AppPlatform {
  android,
  iOS,
}

EffectiveUiMode resolveEffectiveUiMode({
  required UiModePreference preference,
  required AppPlatform targetPlatform,
}) {
  switch (preference) {
    case UiModePreference.apple:
      return EffectiveUiMode.apple;
    case UiModePreference.android:
      return EffectiveUiMode.android;
    case UiModePreference.system:
      return targetPlatform == AppPlatform.iOS
          ? EffectiveUiMode.apple
          : EffectiveUiMode.android;
  }
}

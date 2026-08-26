import 'package:mmuautoqr/app/providers.dart';
import 'package:mmuautoqr/app/ui_mode.dart';
import 'package:mmuautoqr/l10n/app_localizations.dart';
import 'package:mmuautoqr/previews/preview_helpers.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widget_previews.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final appUi = ref.watch(appUiProvider);
    final localizations = AppLocalizations.of(context)!;
    final settingsState = ref.watch(settingsControllerProvider);
    final settings = settingsState.asData?.value;

    return appUi.buildPageScaffold(
      title: localizations.settingsTitle,
      child: settings == null
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: appUi.pagePadding,
              children: [
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Text(
                    localizations.settingsUiMode,
                    style: Theme.of(context).textTheme.titleMedium,
                  ),
                ),
                const SizedBox(height: 12),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: appUi.buildSegmentedControl<UiModePreference>(
                    selectedValue: settings.uiModePreference,
                    options: [
                      (
                        value: UiModePreference.apple,
                        label: localizations.settingsUiModeApple,
                      ),
                      (
                        value: UiModePreference.android,
                        label: localizations.settingsUiModeAndroid,
                      ),
                    ],
                    onSelected: (value) {
                      ref
                          .read(settingsControllerProvider.notifier)
                          .setUiModePreference(value);
                    },
                  ),
                ),
                const SizedBox(height: 24),
                appUi.buildSwitchTile(
                  title: localizations.settingsAutoOpenQrScannerTitle,
                  subtitle: localizations.settingsAutoOpenQrScannerSubtitle,
                  value: settings.autoOpenQrScanner,
                  onChanged: (value) {
                    ref
                        .read(settingsControllerProvider.notifier)
                        .setAutoOpenQrScannerEnabled(value);
                  },
                ),
              ],
            ),
    );
  }
}

@Preview(name: 'Settings Material', size: previewPhoneSize)
Widget settingsMaterialPreview() => buildScreenPreview(
  uiMode: UiModePreference.android,
  child: const SettingsScreen(),
);

@Preview(name: 'Settings Cupertino', size: previewPhoneSize)
Widget settingsCupertinoPreview() => buildScreenPreview(
  uiMode: UiModePreference.apple,
  child: const SettingsScreen(),
);

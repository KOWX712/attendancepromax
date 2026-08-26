import 'package:mmuautoqr/app/app.dart';
import 'package:mmuautoqr/app/providers.dart';
import 'package:mmuautoqr/app/ui_mode.dart';
import 'package:mmuautoqr/core/models/user_record.dart';
import 'package:mmuautoqr/core/repositories/settings_repository.dart';
import 'package:mmuautoqr/core/repositories/user_repository.dart';
import 'package:mmuautoqr/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

const previewPhoneSize = Size(360, 640);

Widget buildScreenPreview({
  required UiModePreference uiMode,
  required Widget child,
}) {
  final settingsRepository = _PreviewSettingsRepository(
    uiModePreference: uiMode,
    autoOpenQrScanner: false,
  );
  final userRepository = _PreviewUserRepository(
    users: const [
      UserRecord(
        id: '1',
        name: 'Alice Tan',
        userId: 'alice01',
        password: '1234',
        isActive: true,
      ),
      UserRecord(
        id: '2',
        name: 'Bob Lim',
        userId: 'bob02',
        password: '5678',
        isActive: false,
      ),
    ],
  );

  return ProviderScope(
    overrides: [
      settingsRepositoryProvider.overrideWithValue(settingsRepository),
      userRepositoryProvider.overrideWithValue(userRepository),
      appPlatformProvider.overrideWithValue(
        uiMode == UiModePreference.apple
            ? AppPlatform.iOS
            : AppPlatform.android,
      ),
    ],
    child: _PreviewBootstrap(child: child),
  );
}

class _PreviewBootstrap extends ConsumerWidget {
  const _PreviewBootstrap({required this.child});

  final Widget child;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    ref.watch(settingsControllerProvider);
    ref.watch(usersControllerProvider);
    return AttendanceProMaxPreviewApp(child: child);
  }
}

class AttendanceProMaxPreviewApp extends ConsumerWidget {
  const AttendanceProMaxPreviewApp({required this.child, super.key});

  final Widget child;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final uiMode = ref.watch(effectiveUiModeProvider);
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ],
      supportedLocales: AppLocalizations.supportedLocales,
      home: AttendanceProMaxAppShell(uiMode: uiMode, child: child),
    );
  }
}

class _PreviewSettingsRepository implements SettingsRepository {
  const _PreviewSettingsRepository({
    required this.uiModePreference,
    required this.autoOpenQrScanner,
  });

  final UiModePreference uiModePreference;
  final bool autoOpenQrScanner;

  @override
  Future<UiModePreference> getUiModePreference() async => uiModePreference;

  @override
  Future<bool> isAutoOpenQrScannerEnabled() async => autoOpenQrScanner;

  @override
  Future<void> setAutoOpenQrScannerEnabled(bool enabled) async {}

  @override
  Future<void> setUiModePreference(UiModePreference preference) async {}
}

class _PreviewUserRepository implements UserRepository {
  const _PreviewUserRepository({required this.users});

  final List<UserRecord> users;

  @override
  Future<UserRecord> addUser({
    required String name,
    required String userId,
    required String password,
  }) async {
    return users.first;
  }

  @override
  Future<void> deleteUser(String id) async {}

  @override
  Future<int> getActiveUserCount() async {
    return users.where((user) => user.isActive).length;
  }

  @override
  Future<int> getUserCount() async => users.length;

  @override
  Future<List<UserRecord>> getUsers() async => users;

  @override
  Future<UserRecord> updateUser(UserRecord user) async => user;
}

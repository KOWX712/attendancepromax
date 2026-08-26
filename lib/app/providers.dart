import 'package:mmuautoqr/app/app_ui.dart';
import 'package:mmuautoqr/app/ui_mode.dart';
import 'package:mmuautoqr/core/models/user_record.dart';
import 'package:mmuautoqr/core/repositories/settings_repository.dart';
import 'package:mmuautoqr/core/repositories/user_repository.dart';
import 'package:mmuautoqr/features/home/home_stats.dart';
import 'package:mmuautoqr/features/settings/app_settings_state.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

final settingsRepositoryProvider = Provider<SettingsRepository>((ref) {
  throw UnimplementedError('settingsRepositoryProvider must be overridden.');
});

final userRepositoryProvider = Provider<UserRepository>((ref) {
  throw UnimplementedError('userRepositoryProvider must be overridden.');
});

final appPlatformProvider = Provider<AppPlatform>((ref) {
  return switch (defaultTargetPlatform) {
    TargetPlatform.iOS => AppPlatform.iOS,
    _ => AppPlatform.android,
  };
});

class SettingsController extends AsyncNotifier<AppSettingsState> {
  @override
  Future<AppSettingsState> build() async {
    final repository = ref.watch(settingsRepositoryProvider);
    final storedUiModePreference = await repository.getUiModePreference();
    final autoOpenQrScanner = await repository.isAutoOpenQrScannerEnabled();
    final platform = ref.watch(appPlatformProvider);
    final uiModePreference = switch (storedUiModePreference) {
      UiModePreference.system => platform == AppPlatform.iOS
          ? UiModePreference.apple
          : UiModePreference.android,
      _ => storedUiModePreference,
    };

    if (storedUiModePreference != uiModePreference) {
      await repository.setUiModePreference(uiModePreference);
    }

    return AppSettingsState(
      uiModePreference: uiModePreference,
      autoOpenQrScanner: autoOpenQrScanner,
    );
  }

  Future<void> setAutoOpenQrScannerEnabled(bool enabled) async {
    final repository = ref.read(settingsRepositoryProvider);
    await repository.setAutoOpenQrScannerEnabled(enabled);
    final currentState = state.asData?.value;
    if (currentState != null) {
      state = AsyncData(currentState.copyWith(autoOpenQrScanner: enabled));
    }
  }

  Future<void> setUiModePreference(UiModePreference preference) async {
    final repository = ref.read(settingsRepositoryProvider);
    await repository.setUiModePreference(preference);
    final currentState = state.asData?.value;
    if (currentState != null) {
      state = AsyncData(currentState.copyWith(uiModePreference: preference));
    }
  }
}

final settingsControllerProvider =
    AsyncNotifierProvider<SettingsController, AppSettingsState>(
      SettingsController.new,
    );

class UsersController extends AsyncNotifier<List<UserRecord>> {
  @override
  Future<List<UserRecord>> build() {
    return ref.watch(userRepositoryProvider).getUsers();
  }

  Future<void> addUser({
    required String name,
    required String userId,
    required String password,
  }) async {
    final repository = ref.read(userRepositoryProvider);
    await repository.addUser(name: name, userId: userId, password: password);
    state = AsyncData(await repository.getUsers());
  }

  Future<void> deleteUser(String id) async {
    final repository = ref.read(userRepositoryProvider);
    await repository.deleteUser(id);
    state = AsyncData(await repository.getUsers());
  }

  Future<void> updateUser(UserRecord user) async {
    final repository = ref.read(userRepositoryProvider);
    await repository.updateUser(user);
    state = AsyncData(await repository.getUsers());
  }
}

final usersControllerProvider =
    AsyncNotifierProvider<UsersController, List<UserRecord>>(
      UsersController.new,
    );

final homeStatsProvider = Provider<HomeStats>((ref) {
  final users =
      ref.watch(usersControllerProvider).asData?.value ?? <UserRecord>[];
  return HomeStats(
    totalUsers: users.length,
    activeUsers: users.where((user) => user.isActive).length,
  );
});

final effectiveUiModeProvider = Provider<EffectiveUiMode>((ref) {
  final platform = ref.watch(appPlatformProvider);
  final settings = ref.watch(settingsControllerProvider).asData?.value;
  return resolveEffectiveUiMode(
    preference: settings?.uiModePreference ?? UiModePreference.system,
    targetPlatform: platform,
  );
});

final appUiProvider = Provider<AppUi>((ref) {
  final mode = ref.watch(effectiveUiModeProvider);
  return AppUi(mode);
});

class LaunchAutoOpenQrScannerHandledController extends Notifier<bool> {
  @override
  bool build() => false;

  void markHandled() {
    state = true;
  }
}

final launchAutoOpenQrScannerHandledProvider =
    NotifierProvider<LaunchAutoOpenQrScannerHandledController, bool>(
      LaunchAutoOpenQrScannerHandledController.new,
    );

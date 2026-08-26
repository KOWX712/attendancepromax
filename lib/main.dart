import 'package:mmuautoqr/app/app.dart';
import 'package:mmuautoqr/app/providers.dart';
import 'package:mmuautoqr/core/repositories/settings_repository.dart';
import 'package:mmuautoqr/core/repositories/user_repository.dart';
import 'package:mmuautoqr/core/storage/secure_key_value_store.dart';
import 'package:mmuautoqr/core/storage/shared_preferences_key_value_store.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final sharedPreferences = await SharedPreferences.getInstance();
  const secureStorage = FlutterSecureStorage();

  runApp(
    ProviderScope(
      overrides: [
        settingsRepositoryProvider.overrideWithValue(
          LocalSettingsRepository(
            SharedPreferencesKeyValueStore(sharedPreferences),
          ),
        ),
        userRepositoryProvider.overrideWithValue(
          LocalUserRepository(
            store: SecureKeyValueStore(secureStorage),
            storageKey: 'users.encrypted.v1',
            idGenerator: const Uuid().v4,
          ),
        ),
      ],
      child: const AttendanceProMaxApp(),
    ),
  );
}

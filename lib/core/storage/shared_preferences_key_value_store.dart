import 'package:mmuautoqr/core/storage/key_value_store.dart';
import 'package:shared_preferences/shared_preferences.dart';

class SharedPreferencesKeyValueStore implements KeyValueStore {
  SharedPreferencesKeyValueStore(this._preferences);

  final SharedPreferences _preferences;

  @override
  Future<void> delete(String key) async {
    await _preferences.remove(key);
  }

  @override
  Future<String?> read(String key) async {
    return _preferences.getString(key);
  }

  @override
  Future<void> write(String key, String? value) async {
    if (value == null) {
      await _preferences.remove(key);
      return;
    }

    await _preferences.setString(key, value);
  }
}

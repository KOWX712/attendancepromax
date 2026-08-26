import 'package:mmuautoqr/core/storage/key_value_store.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SecureKeyValueStore implements KeyValueStore {
  SecureKeyValueStore(this._storage);

  final FlutterSecureStorage _storage;

  @override
  Future<void> delete(String key) {
    return _storage.delete(key: key);
  }

  @override
  Future<String?> read(String key) {
    return _storage.read(key: key);
  }

  @override
  Future<void> write(String key, String? value) {
    return _storage.write(key: key, value: value);
  }
}

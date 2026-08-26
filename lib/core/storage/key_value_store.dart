abstract interface class KeyValueStore {
  Future<String?> read(String key);

  Future<void> write(String key, String? value);

  Future<void> delete(String key);
}

class InMemoryKeyValueStore implements KeyValueStore {
  final Map<String, String> _values = <String, String>{};

  @override
  Future<void> delete(String key) async {
    _values.remove(key);
  }

  @override
  Future<String?> read(String key) async {
    return _values[key];
  }

  @override
  Future<void> write(String key, String? value) async {
    if (value == null) {
      _values.remove(key);
      return;
    }

    _values[key] = value;
  }
}

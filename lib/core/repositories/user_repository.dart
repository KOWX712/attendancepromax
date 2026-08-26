import 'dart:convert';

import 'package:mmuautoqr/core/models/user_record.dart';
import 'package:mmuautoqr/core/storage/key_value_store.dart';

typedef IdGenerator = String Function();

class DuplicateUserIdException implements Exception {
  DuplicateUserIdException(this.userId);

  final String userId;
}

abstract interface class UserRepository {
  Future<List<UserRecord>> getUsers();

  Future<int> getUserCount();

  Future<int> getActiveUserCount();

  Future<UserRecord> addUser({
    required String name,
    required String userId,
    required String password,
  });

  Future<UserRecord> updateUser(UserRecord user);

  Future<void> deleteUser(String id);
}

class LocalUserRepository implements UserRepository {
  LocalUserRepository({
    required KeyValueStore store,
    required this.storageKey,
    required IdGenerator idGenerator,
  })  : _store = store,
        _idGenerator = idGenerator;

  final KeyValueStore _store;
  final String storageKey;
  final IdGenerator _idGenerator;

  @override
  Future<UserRecord> addUser({
    required String name,
    required String userId,
    required String password,
  }) async {
    final users = await getUsers();
    if (users.any((user) => user.userId == userId)) {
      throw DuplicateUserIdException(userId);
    }

    final newUser = UserRecord(
      id: _idGenerator(),
      name: name.trim(),
      userId: userId.trim(),
      password: password.trim(),
      isActive: true,
    );

    await _saveUsers(<UserRecord>[...users, newUser]);
    return newUser;
  }

  @override
  Future<void> deleteUser(String id) async {
    final users = await getUsers();
    await _saveUsers(users.where((user) => user.id != id).toList());
  }

  @override
  Future<int> getActiveUserCount() async {
    final users = await getUsers();
    return users.where((user) => user.isActive).length;
  }

  @override
  Future<int> getUserCount() async {
    final users = await getUsers();
    return users.length;
  }

  @override
  Future<List<UserRecord>> getUsers() async {
    final rawValue = await _store.read(storageKey);
    if (rawValue == null || rawValue.isEmpty) {
      return <UserRecord>[];
    }

    final decoded = jsonDecode(rawValue) as List<dynamic>;
    return decoded
        .map((entry) => UserRecord.fromJson(entry as Map<String, Object?>))
        .toList(growable: false);
  }

  @override
  Future<UserRecord> updateUser(UserRecord user) async {
    final users = await getUsers();
    final userIndex = users.indexWhere((current) => current.id == user.id);
    if (userIndex < 0) {
      return user;
    }

    final currentUser = users[userIndex];
    final updatedUser = currentUser.copyWith(
      name: user.name.trim(),
      isActive: user.isActive,
      password: user.password.trim().isEmpty ? currentUser.password : user.password.trim(),
    );

    final nextUsers = [...users];
    nextUsers[userIndex] = updatedUser;
    await _saveUsers(nextUsers);
    return updatedUser;
  }

  Future<void> _saveUsers(List<UserRecord> users) {
    final encoded = jsonEncode(users.map((user) => user.toJson()).toList());
    return _store.write(storageKey, encoded);
  }
}

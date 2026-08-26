class UserRecord {
  const UserRecord({
    required this.id,
    required this.name,
    required this.userId,
    required this.password,
    required this.isActive,
  });

  final String id;
  final String name;
  final String userId;
  final String password;
  final bool isActive;

  UserRecord copyWith({
    String? id,
    String? name,
    String? userId,
    String? password,
    bool? isActive,
  }) {
    return UserRecord(
      id: id ?? this.id,
      name: name ?? this.name,
      userId: userId ?? this.userId,
      password: password ?? this.password,
      isActive: isActive ?? this.isActive,
    );
  }

  Map<String, Object?> toJson() {
    return <String, Object?>{
      'id': id,
      'name': name,
      'userId': userId,
      'password': password,
      'isActive': isActive,
    };
  }

  factory UserRecord.fromJson(Map<String, Object?> json) {
    return UserRecord(
      id: json['id']! as String,
      name: json['name']! as String,
      userId: json['userId']! as String,
      password: json['password']! as String,
      isActive: json['isActive']! as bool,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is UserRecord &&
        other.id == id &&
        other.name == name &&
        other.userId == userId &&
        other.password == password &&
        other.isActive == isActive;
  }

  @override
  int get hashCode => Object.hash(id, name, userId, password, isActive);
}

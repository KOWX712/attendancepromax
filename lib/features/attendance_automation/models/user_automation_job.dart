import 'package:mmuautoqr/core/models/user_record.dart';

/// A single user attendance automation job queued for processing.
class UserAutomationJob {
  const UserAutomationJob({
    required this.user,
    required this.qrUrl,
    this.attemptCount = 0,
  });

  final UserRecord user;
  final String qrUrl;
  final int attemptCount;

  UserAutomationJob copyWith({
    UserRecord? user,
    String? qrUrl,
    int? attemptCount,
  }) {
    return UserAutomationJob(
      user: user ?? this.user,
      qrUrl: qrUrl ?? this.qrUrl,
      attemptCount: attemptCount ?? this.attemptCount,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is UserAutomationJob &&
        other.user.id == user.id &&
        other.qrUrl == qrUrl &&
        other.attemptCount == attemptCount;
  }

  @override
  int get hashCode => Object.hash(user.id, qrUrl, attemptCount);

  @override
  String toString() =>
      'UserAutomationJob(user: ${user.id}, qrUrl: $qrUrl, attemptCount: $attemptCount)';
}

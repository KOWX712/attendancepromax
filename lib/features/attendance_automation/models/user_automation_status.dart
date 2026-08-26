import 'package:mmuautoqr/core/models/user_record.dart';

/// Lifecycle state of a user automation job in the worker queue.
enum UserAutomationStatus {
  /// Job is queued and waiting for the worker to pick it up.
  queued,

  /// Worker is actively processing this user's attendance submission.
  loading,

  /// Attendance submission succeeded; job complete.
  success,

  /// Attendance submission failed (login error, form rejection, network error).
  failed,

  /// QR code expired or invalid; triggers global automation abort.
  expiredQr,
}

extension UserAutomationStatusLabel on UserAutomationStatus {
  /// Human-readable label for UI display.
  String get label {
    return switch (this) {
      UserAutomationStatus.queued => 'Queued',
      UserAutomationStatus.loading => 'Processing',
      UserAutomationStatus.success => 'Success',
      UserAutomationStatus.failed => 'Failed',
      UserAutomationStatus.expiredQr => 'QR Expired',
    };
  }
}

/// Result of a user automation attempt with optional error details.
class UserAutomationResult {
  const UserAutomationResult({
    required this.user,
    required this.status,
    this.errorMessage,
  });

  final UserRecord user;
  final UserAutomationStatus status;
  final String? errorMessage;

  UserAutomationResult copyWith({
    UserRecord? user,
    UserAutomationStatus? status,
    String? errorMessage,
  }) {
    return UserAutomationResult(
      user: user ?? this.user,
      status: status ?? this.status,
      errorMessage: errorMessage ?? this.errorMessage,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is UserAutomationResult &&
        other.user.id == user.id &&
        other.status == status;
  }

  @override
  int get hashCode => Object.hash(user.id, status);

  @override
  String toString() =>
      'UserAutomationResult(user: ${user.id}, status: ${status.label}, errorMessage: $errorMessage)';
}

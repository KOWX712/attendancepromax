import '../models/user_automation_job.dart';
import '../models/user_automation_status.dart';

/// Contract every pooled automation worker fulfils. One worker wraps one
/// long-lived browser engine instance that is reused across many jobs.
abstract interface class AutomationWorker {
  /// Runs [job] to completion. Never throws for expected automation
  /// outcomes - failures are reported via [UserAutomationResult.status].
  Future<UserAutomationResult> executeJob(UserAutomationJob job);

  /// Tears down the underlying engine instance. Call once per worker,
  /// only when its current (and last) job has fully settled.
  Future<void> dispose();
}

typedef AutomationWorkerFactory = AutomationWorker Function();

import 'dart:async';
import 'dart:collection';

import 'package:mmuautoqr/core/models/user_record.dart';
import 'package:mmuautoqr/features/attendance_automation/models/user_automation_job.dart';
import 'package:mmuautoqr/features/attendance_automation/models/user_automation_status.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

import 'automation_worker.dart';

/// Pure-Dart orchestrator managing a pool of up to [maxConcurrentWorkers]
/// HeadlessInAppWebView workers executing QR-checkin jobs for multiple users.
///
/// **Dispatch Rules (REUSE-NOT-RECREATE)**:
/// - Workers are created LAZILY up to the cap as jobs become available.
/// - Once a worker completes a job, it is REUSED for the next queued job
///   (never disposed mid-run) because QR lifetime is short and recreation
///   cost is high.
/// - Workers are NEVER reused across runs: a new [startAutomation] always
///   builds a fresh pool after tearing down the previous one.
/// - NO early disposal: all workers stay alive until every enqueued job has
///   a terminal status (or abort settles).
///
/// **Thread Safety**:
/// Relies on Dart's single-threaded event loop. The [_cookieGate] chain
/// serializes pre-dispatch cookie wipes so that concurrent workers cannot
/// interleave their cookie cleanup with another worker's page load.
class AttendanceAutomationOrchestrator {
  AttendanceAutomationOrchestrator({
    required AutomationWorkerFactory workerFactory,
    int maxConcurrentWorkers = 3,
  })  : _workerFactory = workerFactory,
        _maxConcurrentWorkers = maxConcurrentWorkers {
    assert(
      maxConcurrentWorkers >= 1,
      'maxConcurrentWorkers must be at least 1',
    );
  }

  final AutomationWorkerFactory _workerFactory;
  final int _maxConcurrentWorkers;

  final StreamController<Map<String, UserAutomationResult>>
      _statusController = StreamController.broadcast();

  /// Live status stream emitting full snapshots on every change.
  Stream<Map<String, UserAutomationResult>> get statusStream =>
      _statusController.stream;

  /// Synchronous access to current status map (copy).
  Map<String, UserAutomationResult> get currentStatus =>
      Map.unmodifiable(_statusMap);

  // Internal state
  final Queue<UserAutomationJob> _jobQueue = Queue();
  final Map<String, UserAutomationResult> _statusMap = {};
  final List<AutomationWorker> _activeWorkers = [];
  final Set<Future<void>> _inFlightJobs = {};

  bool _aborted = false;
  bool _isDisposed = false;

  /// Serialization gate for cookie cleanup before job dispatch.
  Future<void> _cookieGate = Future.value();

  /// Starts automation for [users] using [qrUrl]. Full reset of any previous run.
  Future<void> startAutomation({
    required String qrUrl,
    required List<UserRecord> users,
  }) async {
    if (users.isEmpty) {
      return; // Graceful no-op for empty user lists
    }

    // Full reset: tear down any previous run
    await _disposeRun();

    _aborted = false;
    _jobQueue.clear();
    _statusMap.clear();
    _activeWorkers.clear();
    _inFlightJobs.clear();
    _cookieGate = Future.value();

    // Enqueue jobs in FIFO order
    for (final user in users) {
      final job = UserAutomationJob(user: user, qrUrl: qrUrl);
      _jobQueue.add(job);
      _statusMap[user.id] = UserAutomationResult(
        user: user,
        status: UserAutomationStatus.queued,
      );
    }

    // Broadcast initial snapshot
    _emitSnapshot();

    // Start dispatch loop
    unawaited(_dispatchLoop());
  }

  /// Core dispatch loop: creates workers up to cap, reuses them after completion.
  Future<void> _dispatchLoop() async {
    while (_jobQueue.isNotEmpty && !_aborted) {
      // Create new worker if below cap and have pending work
      if (_activeWorkers.length < _maxConcurrentWorkers &&
          _activeWorkers.length < _jobQueue.length + _inFlightJobs.length) {
        final worker = _workerFactory();
        _activeWorkers.add(worker);
        final job = _jobQueue.removeFirst();
        unawaited(_executeJob(worker, job));
      } else if (_inFlightJobs.isNotEmpty) {
        // At cap: wait for any in-flight job to settle, then reuse that worker
        await Future.any(_inFlightJobs);
      } else {
        // Should not reach here, but break to avoid infinite loop
        break;
      }
    }

    // Wait for all in-flight jobs to settle
    if (_inFlightJobs.isNotEmpty) {
      await Future.wait(_inFlightJobs);
    }

    // Run complete: tear down pool
    await _teardownPool();
  }

  /// Executes [job] on [worker], handling cookie cleanup, result emission, and reuse.
  Future<void> _executeJob(AutomationWorker worker, UserAutomationJob job) async {
    final jobFuture = _wrapJobExecution(worker, job);
    _inFlightJobs.add(jobFuture);

    try {
      await jobFuture;
    } finally {
      _inFlightJobs.remove(jobFuture);

      // Reuse this worker for next job if available and not aborted
      if (_jobQueue.isNotEmpty && !_aborted) {
        final nextJob = _jobQueue.removeFirst();
        unawaited(_executeJob(worker, nextJob));
      }
    }
  }

  /// Wraps a single job execution: loading status → cookie cleanup → execution → result.
  Future<void> _wrapJobExecution(AutomationWorker worker, UserAutomationJob job) async {
    final userId = job.user.id;

    try {
      // Emit loading status
      _updateStatus(
        userId,
        UserAutomationResult(
          user: job.user,
          status: UserAutomationStatus.loading,
        ),
      );

      // Serialize cookie cleanup through the gate
      await _performCookieCleanup(job.qrUrl);

      // Execute job
      final result = await worker.executeJob(job);

      // Handle result
      if (_aborted) {
        // Discard late results during abort
        return;
      }

      _updateStatus(userId, result);

      // Global abort on QR expiration
      if (result.status == UserAutomationStatus.expiredQr) {
        unawaited(_triggerGlobalAbort(reason: 'QR code expired'));
      }
    } catch (e) {
      // Unexpected worker exception: convert to failed status
      if (!_aborted) {
        _updateStatus(
          userId,
          UserAutomationResult(
            user: job.user,
            status: UserAutomationStatus.failed,
            errorMessage: e.toString(),
          ),
        );
      }
      // Error silently handled; concrete worker should log
    }
  }

  /// Serialized cookie cleanup before job dispatch.
  /// CookieManager API verified from flutter_inappwebview 6.1.5 source:
  /// CookieManager.instance() returns singleton (line 40 of cookie_manager.dart)
  /// deleteCookies(url: WebUri) exists (lines 124-136)
  Future<void> _performCookieCleanup(String qrUrl) async {
    _cookieGate = _cookieGate.then((_) async {
      try {
        final uri = Uri.parse(qrUrl);
        final origin = '${uri.scheme}://${uri.host}';
        await CookieManager.instance().deleteCookies(url: WebUri(origin));
      } catch (e) {
        // Cookie cleanup failure never blocks job execution (silently handled)
      }
    });

    await _cookieGate;
  }

  /// Triggers global abort: drains queue, discards in-flight results.
  Future<void> _triggerGlobalAbort({required String reason}) async {
    if (_aborted) {
      return; // Already aborted
    }

    _aborted = true;

    // Drain queue: mark all remaining as expiredQr or failed
    while (_jobQueue.isNotEmpty) {
      final job = _jobQueue.removeFirst();
      _updateStatus(
        job.user.id,
        UserAutomationResult(
          user: job.user,
          status: UserAutomationStatus.expiredQr,
          errorMessage: reason,
        ),
      );
    }

    // In-flight jobs will settle naturally; their results are discarded in _wrapJobExecution
  }

  /// Public abort: user-initiated cancellation.
  Future<void> abortAll() async {
    await _triggerGlobalAbort(reason: 'Aborted by user');
  }

  /// Updates status map and emits snapshot.
  void _updateStatus(String userId, UserAutomationResult result) {
    if (_isDisposed || _statusController.isClosed) {
      return;
    }

    _statusMap[userId] = result;
    _emitSnapshot();
  }

  /// Emits a full status snapshot (copy).
  void _emitSnapshot() {
    if (_isDisposed || _statusController.isClosed) {
      return;
    }

    _statusController.add(Map.from(_statusMap));
  }

  /// Tears down the worker pool: disposes all workers sequentially.
  Future<void> _teardownPool() async {
    for (final worker in _activeWorkers) {
      try {
        await worker.dispose();
      } catch (e) {
        // Worker disposal failure is non-fatal (silently handled)
      }
    }

    _activeWorkers.clear();
  }

  /// Disposes the current run: aborts if running, tears down pool.
  Future<void> _disposeRun() async {
    if (_jobQueue.isNotEmpty || _inFlightJobs.isNotEmpty) {
      await abortAll();

      // Wait for all in-flight jobs to settle
      if (_inFlightJobs.isNotEmpty) {
        await Future.wait(_inFlightJobs);
      }
    }

    await _teardownPool();
  }

  /// Full shutdown: disposes run and closes stream controller.
  Future<void> dispose() async {
    if (_isDisposed) {
      return;
    }

    _isDisposed = true;

    await _disposeRun();

    if (!_statusController.isClosed) {
      await _statusController.close();
    }
  }
}

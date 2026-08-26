import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';

import '../../attendance_webview/attendance_automation.dart';
import '../helpers/attendance_page_classifier.dart';
import '../helpers/javascript_result_decoding.dart';
import '../models/user_automation_job.dart';
import '../models/user_automation_status.dart';
import 'automation_worker.dart';

/// Silent background worker executing QR-checkin automation via a single
/// long-lived HeadlessInAppWebView instance, reused across sequential jobs.
///
/// WHY HEADLESS: QR codes expire quickly (often <60s); the legacy visible
/// WebView flow forces users to watch each submission. This worker runs jobs
/// silently in the background, allowing immediate UI return while preserving
/// the proven behavioral contracts (2s injection delay, stale-event rejection,
/// intermediate-page retry, expired-QR detection).
///
/// LIFECYCLE: First [executeJob] lazily creates the HeadlessInAppWebView and
/// calls run(). Subsequent jobs reuse the same instance—only loadUrl() changes.
/// The engine persists until [dispose()] is called (once, when idle).
///
/// CONCURRENCY: Enforced by orchestrator—only one job active at a time.
class HeadlessAutomationWorker implements AutomationWorker {
  HeadlessAutomationWorker({
    required String automationScriptTemplate,
    this.classifier = const AttendancePageClassifier(),
  }) : _automationScriptTemplate = automationScriptTemplate;

  final String _automationScriptTemplate;
  final AttendancePageClassifier classifier;

  HeadlessInAppWebView? _headlessWebView;
  InAppWebViewController? _webViewController;

  /// Per-worker run counter for stale-event rejection via automation.js runId.
  int _runId = 0;

  /// Per-job state—reset on each executeJob() entry.
  bool _hasRetriedIntermediatePage = false;
  final Set<String> _processedLoadStopKeys = {};
  bool _submittedHandled = false;
  bool _loadIndicatorHiddenSeen = false;
  Completer<UserAutomationResult>? _jobCompleter;
  Timer? _injectionTimer;
  Timer? _advanceTimer;
  Timer? _watchdogTimer;
  UserAutomationJob? _currentJob;

  @override
  Future<UserAutomationResult> executeJob(UserAutomationJob job) async {
    try {
      // Reset per-job state.
      _runId += 1;
      _hasRetriedIntermediatePage = false;
      _processedLoadStopKeys.clear();
      _submittedHandled = false;
      _loadIndicatorHiddenSeen = false;
      _currentJob = job;
      _jobCompleter = Completer<UserAutomationResult>();

      // Cancel any lingering timers from a previous job (defensive).
      _injectionTimer?.cancel();
      _advanceTimer?.cancel();
      _watchdogTimer?.cancel();

      // Lazy engine creation on first job.
      if (_headlessWebView == null) {
        await _createHeadlessEngine();
      }

      // Start watchdog: 45s hard timeout per job.
      _watchdogTimer = Timer(const Duration(seconds: 45), () {
        if (_jobCompleter != null && !_jobCompleter!.isCompleted) {
          _webViewController?.stopLoading();
          _completeJob(
            UserAutomationResult(
              user: job.user,
              status: UserAutomationStatus.failed,
              errorMessage: 'Timed out waiting for check-in to finish',
            ),
          );
        }
      });

      // Load the QR URL (cookie hygiene handled upstream by orchestrator).
      final controller = _webViewController;
      if (controller == null) {
        return UserAutomationResult(
          user: job.user,
          status: UserAutomationStatus.failed,
          errorMessage: 'WebView controller unavailable after engine creation',
        );
      }

      await controller.loadUrl(
        urlRequest: URLRequest(url: WebUri(job.qrUrl)),
      );

      return await _jobCompleter!.future;
    } catch (error, stackTrace) {
      debugPrint('HeadlessAutomationWorker.executeJob exception: $error\n$stackTrace');
      return UserAutomationResult(
        user: job.user,
        status: UserAutomationStatus.failed,
        errorMessage: 'Unexpected error: $error',
      );
    }
  }

  @override
  Future<void> dispose() async {
    _injectionTimer?.cancel();
    _advanceTimer?.cancel();
    _watchdogTimer?.cancel();

    final controller = _webViewController;
    if (controller != null) {
      // Clean up JavaScript handler registration.
      if (controller.hasJavaScriptHandler(handlerName: 'attendanceBridge')) {
        controller.removeJavaScriptHandler(handlerName: 'attendanceBridge');
      }
    }

    await _headlessWebView?.dispose();
    _headlessWebView = null;
    _webViewController = null;
  }

  /// Creates and runs the HeadlessInAppWebView instance, registering all
  /// event handlers once. Subsequent jobs reuse this engine.
  Future<void> _createHeadlessEngine() async {
    _headlessWebView = HeadlessInAppWebView(
      initialSettings: InAppWebViewSettings(
        javaScriptEnabled: true,
        transparentBackground: true,
        cacheEnabled: true,
        supportZoom: false,
      ),
      onWebViewCreated: (controller) {
        _webViewController = controller;
        // Register JavaScript handler ONCE for the lifetime of this engine.
        controller.addJavaScriptHandler(
          handlerName: 'attendanceBridge',
          callback: _handleBridgeMessage,
        );
      },
      onLoadStop: _handleLoadStop,
      onReceivedError: _handleReceivedError,
      onConsoleMessage: _handleConsoleMessage,
    );

    await _headlessWebView!.run();
  }

  /// Called when automation.js posts a message via `window.flutter_inappwebview.callHandler`.
  /// Signature for flutter_inappwebview 6.1.5: `JavaScriptHandlerCallback(List<dynamic> arguments)`.
  dynamic _handleBridgeMessage(List<dynamic> arguments) {
    if (arguments.isEmpty) {
      return null;
    }

    final message = arguments.first;
    if (message is! Map) {
      return null;
    }

    final runId = message['runId'] as int?;
    if (runId != _runId) {
      // Stale event from a previous job's injection—ignore.
      debugPrint('[HeadlessAutomationWorker] Ignoring stale bridge message (runId $runId != current $_runId)');
      return null;
    }

    final type = message['type'] as String?;
    final reason = message['reason'] as String?;

    if (type == 'submitted') {
      _submittedHandled = true;
      debugPrint('[HeadlessAutomationWorker] Form submitted for user ${_currentJob?.user.userId}');

      // Legacy flow advanced 1s later; we replicate that timing for the
      // success-completion fallback. However, load_indicator_hidden may
      // arrive first and complete immediately.
      _advanceTimer?.cancel();
      _advanceTimer = Timer(const Duration(seconds: 4), () {
        // If load_indicator_hidden hasn't arrived after 4s, complete anyway.
        if (!_loadIndicatorHiddenSeen) {
          _completeJobIfNotSettled(
            UserAutomationResult(
              user: _currentJob!.user,
              status: UserAutomationStatus.success,
            ),
          );
        }
      });
    } else if (type == 'load_indicator_hidden') {
      _loadIndicatorHiddenSeen = true;
      debugPrint('[HeadlessAutomationWorker] Load indicator hidden');

      // If submitted was already seen, complete success immediately.
      // Otherwise, wait for submitted.
      if (_submittedHandled) {
        _advanceTimer?.cancel();
        _completeJobIfNotSettled(
          UserAutomationResult(
            user: _currentJob!.user,
            status: UserAutomationStatus.success,
          ),
        );
      }
    } else if (type == 'failed') {
      _completeJobIfNotSettled(
        UserAutomationResult(
          user: _currentJob!.user,
          status: UserAutomationStatus.failed,
          errorMessage: reason ?? 'Automation reported failure',
        ),
      );
    }

    return null;
  }

  /// Called when a page finishes loading. Handles expired-QR detection,
  /// intermediate-page retry, and script injection.
  ///
  /// DEDUPLICATION: onLoadStop fires multiple times per logical navigation
  /// in flutter_inappwebview (e.g., redirects, iframes). We dedupe per
  /// `(url|runId)` except after explicit reload for retry.
  Future<void> _handleLoadStop(
    InAppWebViewController controller,
    WebUri? url,
  ) async {
    final job = _currentJob;
    if (job == null || _jobCompleter == null || _jobCompleter!.isCompleted) {
      return;
    }

    final urlString = url?.toString() ?? '';

    // Read current URL from evaluateJavascript (more reliable across redirects).
    String currentUrl = urlString;
    try {
      final result = await controller.evaluateJavascript(
        source: 'window.location.href',
      );
      if (result is String && result.isNotEmpty && result != 'null') {
        currentUrl = result;
      }
    } catch (e) {
      debugPrint('[HeadlessAutomationWorker] Failed to read window.location.href: $e');
    }

    // Dedupe: skip if we've already processed this `(url|runId)` combination,
    // UNLESS we just cleared cache and reloaded (intermediate retry).
    final loadStopKey = '$currentUrl|$_runId';
    if (_processedLoadStopKeys.contains(loadStopKey)) {
      return;
    }
    _processedLoadStopKeys.add(loadStopKey);

    // Capture rendered HTML for classification.
    String renderedHtml = '';
    try {
      final result = await controller.evaluateJavascript(
        source: '(function(){return document.documentElement.outerHTML;})()',
      );
      renderedHtml = decodeJavaScriptHtmlResult(result);
    } catch (e) {
      debugPrint('[HeadlessAutomationWorker] Failed to capture HTML: $e');
    }

    // EXPIRED QR: immediate failure, stop loading.
    if (classifier.isExpiredQrPage(renderedHtml: renderedHtml)) {
      await controller.stopLoading();
      _completeJobIfNotSettled(
        UserAutomationResult(
          user: job.user,
          status: UserAutomationStatus.expiredQr,
          errorMessage: 'QR code expired or invalid',
        ),
      );
      return;
    }

    // INTERMEDIATE PAGE: Oracle SSO bootstrap page with cmd=login & errorCode=105.
    // Retry once by clearing cache and reloading original QR URL.
    if (classifier.isIntermediateAttendanceLoginPage(
      requestedUrl: job.qrUrl,
      renderedUrl: currentUrl,
      renderedHtml: renderedHtml,
    )) {
      if (classifier.shouldRetryIntermediateAttendancePage(
        requestedUrl: job.qrUrl,
        renderedUrl: currentUrl,
        hasRetriedIntermediatePage: _hasRetriedIntermediatePage,
      )) {
        _hasRetriedIntermediatePage = true;
        debugPrint('[HeadlessAutomationWorker] Retrying intermediate page: clearing cache and reloading');
        // Legacy flow used clearCache() (deprecated in 6.1.5 but still functional).
        // We preserve the exact behavior: instance method, not static clearAllCache.
        // ignore: deprecated_member_use
        await controller.clearCache();
        await Future<void>.delayed(const Duration(milliseconds: 50));
        // Clear processed keys so the retry's onLoadStop is NOT deduped.
        _processedLoadStopKeys.clear();
        await controller.loadUrl(
          urlRequest: URLRequest(url: WebUri(job.qrUrl)),
        );
        return;
      } else {
        // Already retried once; fail.
        _completeJobIfNotSettled(
          UserAutomationResult(
            user: job.user,
            status: UserAutomationStatus.failed,
            errorMessage: 'Stuck on intermediate login page after retry',
          ),
        );
        return;
      }
    }

    // INJECT AUTOMATION SCRIPT: 2s delay (matches legacy flow).
    _injectionTimer?.cancel();
    _injectionTimer = Timer(const Duration(seconds: 2), () async {
      if (_jobCompleter == null || _jobCompleter!.isCompleted) {
        return;
      }

      final renderedScript = renderAutomationScriptTemplate(
        template: _automationScriptTemplate,
        userId: job.user.userId,
        password: job.user.password,
        automationRunId: _runId,
      );

      try {
        await controller.evaluateJavascript(source: renderedScript);
        await controller.evaluateJavascript(source: 'window.automation.init();');
        debugPrint('[HeadlessAutomationWorker] Injected automation script for user ${job.user.userId}');
      } catch (e) {
        debugPrint('[HeadlessAutomationWorker] Script injection failed: $e');
        _completeJobIfNotSettled(
          UserAutomationResult(
            user: job.user,
            status: UserAutomationStatus.failed,
            errorMessage: 'Failed to inject automation script: $e',
          ),
        );
      }
    });
  }

  /// Called on main-frame load errors. We log but do NOT fail the job unless
  /// it's the initial QR URL load and job is still early (before login).
  void _handleReceivedError(
    InAppWebViewController controller,
    WebResourceRequest request,
    WebResourceError error,
  ) {
    // Only fail if it's the main frame and job hasn't progressed.
    final job = _currentJob;
    if (job != null &&
        request.isForMainFrame == true &&
        !_submittedHandled &&
        _jobCompleter != null &&
        !_jobCompleter!.isCompleted) {
      debugPrint('[HeadlessAutomationWorker] Main frame error: ${error.description}');
      // Lenient: log but don't auto-fail unless it's clearly unrecoverable.
      // The watchdog will eventually time out if the page never loads.
    }
  }

  /// Routes console.log messages from automation.js (fallback when
  /// window.flutter_inappwebview unavailable) to debugPrint for diagnosability.
  void _handleConsoleMessage(
    InAppWebViewController controller,
    ConsoleMessage consoleMessage,
  ) {
    final message = consoleMessage.message;
    if (message.contains('[automation]')) {
      debugPrint('[HeadlessAutomationWorker] JS Console: $message');
    }
  }

  /// Completes the current job if not already completed (idempotent guard).
  void _completeJobIfNotSettled(UserAutomationResult result) {
    if (_jobCompleter != null && !_jobCompleter!.isCompleted) {
      _completeJob(result);
    }
  }

  /// Completes the job, cancels all timers, and clears job state.
  void _completeJob(UserAutomationResult result) {
    _injectionTimer?.cancel();
    _advanceTimer?.cancel();
    _watchdogTimer?.cancel();

    _injectionTimer = null;
    _advanceTimer = null;
    _watchdogTimer = null;

    _jobCompleter?.complete(result);
    _jobCompleter = null;
    _currentJob = null;
  }
}

import 'dart:async';

import 'package:mmuautoqr/features/attendance_automation/models/user_automation_status.dart';
import 'package:mmuautoqr/features/attendance_automation/services/attendance_automation_orchestrator.dart';
import 'package:mmuautoqr/features/attendance_automation/services/headless_automation_worker.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Loads the automation template once and owns the orchestrator for the app.
///
/// This provider is intentionally keep-alive. The orchestrator owns native
/// headless WebView instances, which can consume roughly 100 MB each and must
/// not be garbage-collected mid-run when a transient widget unmounts, such as
/// during a dialog overlay or brief route push. Runs are short-lived and
/// bounded; the orchestrator tears down completed runs and this provider
/// disposes it when the container shuts down.
final automationOrchestratorProvider =
    FutureProvider<AttendanceAutomationOrchestrator>((ref) async {
      final automationScriptTemplate = await rootBundle.loadString(
        'assets/automation.js',
      );
      final orchestrator = AttendanceAutomationOrchestrator(
        workerFactory: () => HeadlessAutomationWorker(
          automationScriptTemplate: automationScriptTemplate,
        ),
      );
      ref.onDispose(() => unawaited(orchestrator.dispose()));
      return orchestrator;
    });

/// Exposes full status snapshots from the app-owned orchestrator.
///
/// This provider is also intentionally keep-alive for the same reason as
/// [automationOrchestratorProvider]: transient UI unmounts must not dispose
/// native headless WebView resources while a bounded run is active.
final automationStatusProvider =
    StreamProvider<Map<String, UserAutomationResult>>((ref) {
      return ref.watch(automationOrchestratorProvider).asData?.value.statusStream ??
          const Stream<Map<String, UserAutomationResult>>.empty();
    });

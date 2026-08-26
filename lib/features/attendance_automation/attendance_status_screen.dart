import 'package:mmuautoqr/app/providers.dart';
import 'package:mmuautoqr/core/models/user_record.dart';
import 'package:mmuautoqr/features/attendance_automation/models/user_automation_status.dart';
import 'package:mmuautoqr/features/attendance_automation/providers/automation_providers.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class AttendanceStatusScreen extends ConsumerWidget {
  const AttendanceStatusScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final theme = Theme.of(context);
    final colorScheme = theme.colorScheme;
    final usersState = ref.watch(usersControllerProvider);
    final automationStatusState = ref.watch(automationStatusProvider);

    final allUsers = usersState.asData?.value ?? <UserRecord>[];
    final activeUsers =
        allUsers.where((user) => user.isActive).toList(growable: false);

    return PopScope(
      canPop: _canPop(automationStatusState),
      onPopInvokedWithResult: (didPop, result) async {
        if (!didPop && context.mounted) {
          final shouldStop = await _showStopConfirmationDialog(context);
          if (shouldStop && context.mounted) {
            await ref.read(automationOrchestratorProvider.future).then(
              (orchestrator) => orchestrator.abortAll(),
            );
            if (context.mounted) {
              context.go('/');
            }
          }
        }
      },
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Check-in Status'),
        ),
        body: automationStatusState.when(
          data: (statusMap) {
            final stats = _computeStats(activeUsers, statusMap);
            final hasAnyExpiredQr = stats.expiredQrCount > 0;
            final allSuccess = stats.successCount == activeUsers.length &&
                activeUsers.isNotEmpty;
            final hasAnyFailedOrExpired =
                (stats.failedCount > 0 || stats.expiredQrCount > 0);
            final hasAnyActive =
                (stats.queuedCount > 0 || stats.loadingCount > 0);

            return Column(
              children: [
                // Summary header
                Container(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${stats.successCount} of ${activeUsers.length} checked in',
                        style: theme.textTheme.titleMedium,
                      ),
                      const SizedBox(height: 8),
                      LinearProgressIndicator(
                        value: activeUsers.isEmpty
                            ? 0.0
                            : stats.successCount / activeUsers.length,
                      ),
                    ],
                  ),
                ),

                // QR expired banner
                if (hasAnyExpiredQr)
                  Card(
                    margin: const EdgeInsets.symmetric(horizontal: 16),
                    color: const Color(0xFFF57C00),
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.qr_code_scanner,
                            color: Colors.white,
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Text(
                              'The QR code expired before everyone was checked in. Scan a fresh QR code to continue.',
                              style: theme.textTheme.bodyMedium?.copyWith(
                                color: Colors.white,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),

                const SizedBox(height: 8),

                // User list
                Expanded(
                  child: ListView.builder(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    itemCount: activeUsers.length,
                    itemBuilder: (context, index) {
                      final user = activeUsers[index];
                      final result = statusMap[user.userId];
                      final status =
                          result?.status ?? UserAutomationStatus.queued;
                      final errorMessage = result?.errorMessage;

                      return Card(
                        margin: const EdgeInsets.only(bottom: 8),
                        child: ListTile(
                          title: Text(user.name),
                          subtitle: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(user.userId),
                              if (errorMessage != null &&
                                  (status == UserAutomationStatus.failed ||
                                      status ==
                                          UserAutomationStatus.expiredQr)) ...[
                                const SizedBox(height: 4),
                                Text(
                                  errorMessage,
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                  style: TextStyle(
                                    color: colorScheme.error,
                                    fontSize: 12,
                                  ),
                                ),
                              ],
                            ],
                          ),
                          trailing: _buildStatusIndicator(
                            context,
                            status,
                            colorScheme,
                          ),
                        ),
                      );
                    },
                  ),
                ),

                // Bottom actions
                SafeArea(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: _buildBottomAction(
                      context,
                      ref,
                      allSuccess: allSuccess,
                      hasAnyFailedOrExpired: hasAnyFailedOrExpired,
                      hasAnyActive: hasAnyActive,
                    ),
                  ),
                ),
              ],
            );
          },
          loading: () => const Center(
            child: CircularProgressIndicator(),
          ),
          error: (error, stack) => Center(
            child: Text('Error: $error'),
          ),
        ),
      ),
    );
  }

  bool _canPop(AsyncValue<Map<String, UserAutomationResult>> statusState) {
    if (statusState is! AsyncData<Map<String, UserAutomationResult>>) {
      return false;
    }

    final statusMap = statusState.value;
    return !statusMap.values.any(
      (result) =>
          result.status == UserAutomationStatus.queued ||
          result.status == UserAutomationStatus.loading,
    );
  }

  Widget _buildStatusIndicator(
    BuildContext context,
    UserAutomationStatus status,
    ColorScheme colorScheme,
  ) {
    switch (status) {
      case UserAutomationStatus.queued:
        return Tooltip(
          message: 'Status: Queued',
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.schedule,
                color: Colors.grey[600],
              ),
              const SizedBox(height: 2),
              Text(
                'Queued',
                style: TextStyle(
                  fontSize: 11,
                  color: Colors.grey[600],
                ),
              ),
            ],
          ),
        );

      case UserAutomationStatus.loading:
        return Tooltip(
          message: 'Status: Processing',
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                'Processing',
                style: TextStyle(
                  fontSize: 11,
                  color: colorScheme.primary,
                ),
              ),
            ],
          ),
        );

      case UserAutomationStatus.success:
        return Tooltip(
          message: 'Status: Done',
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.check_circle,
                color: Color(0xFF2E7D32),
              ),
              const SizedBox(height: 2),
              const Text(
                'Done',
                style: TextStyle(
                  fontSize: 11,
                  color: Color(0xFF2E7D32),
                ),
              ),
            ],
          ),
        );

      case UserAutomationStatus.failed:
        return Tooltip(
          message: 'Status: Failed',
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                Icons.cancel,
                color: colorScheme.error,
              ),
              const SizedBox(height: 2),
              Text(
                'Failed',
                style: TextStyle(
                  fontSize: 11,
                  color: colorScheme.error,
                ),
              ),
            ],
          ),
        );

      case UserAutomationStatus.expiredQr:
        return Tooltip(
          message: 'Status: QR expired',
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.qr_code_scanner,
                color: Color(0xFFF57C00),
              ),
              const SizedBox(height: 2),
              const Text(
                'QR expired',
                style: TextStyle(
                  fontSize: 11,
                  color: Color(0xFFF57C00),
                ),
              ),
            ],
          ),
        );
    }
  }

  Widget _buildBottomAction(
    BuildContext context,
    WidgetRef ref, {
    required bool allSuccess,
    required bool hasAnyFailedOrExpired,
    required bool hasAnyActive,
  }) {
    if (allSuccess) {
      return SizedBox(
        width: double.infinity,
        child: FilledButton(
          onPressed: () => context.go('/'),
          child: const Text('Done'),
        ),
      );
    }

    if (hasAnyFailedOrExpired && !hasAnyActive) {
      return SizedBox(
        width: double.infinity,
        child: FilledButton.icon(
          onPressed: () async {
            await ref.read(automationOrchestratorProvider.future).then(
              (orchestrator) => orchestrator.abortAll(),
            );
            if (context.mounted) {
              context.go('/scanner');
            }
          },
          icon: const Icon(Icons.qr_code_scanner),
          label: const Text('Scan Again'),
        ),
      );
    }

    if (hasAnyActive) {
      return SizedBox(
        width: double.infinity,
        child: OutlinedButton(
          onPressed: () async {
            final shouldStop = await _showStopConfirmationDialog(context);
            if (shouldStop && context.mounted) {
              await ref.read(automationOrchestratorProvider.future).then(
                (orchestrator) => orchestrator.abortAll(),
              );
              if (context.mounted) {
                context.go('/');
              }
            }
          },
          child: const Text('Cancel'),
        ),
      );
    }

    return const SizedBox.shrink();
  }

  Future<bool> _showStopConfirmationDialog(BuildContext context) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Stop check-in?'),
        content: const Text('Users still processing will be cancelled.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Stop'),
          ),
        ],
      ),
    );
    return result ?? false;
  }

  ({
    int queuedCount,
    int loadingCount,
    int successCount,
    int failedCount,
    int expiredQrCount,
  }) _computeStats(
    List<UserRecord> activeUsers,
    Map<String, UserAutomationResult> statusMap,
  ) {
    int queuedCount = 0;
    int loadingCount = 0;
    int successCount = 0;
    int failedCount = 0;
    int expiredQrCount = 0;

    for (final user in activeUsers) {
      final result = statusMap[user.userId];
      final status = result?.status ?? UserAutomationStatus.queued;

      switch (status) {
        case UserAutomationStatus.queued:
          queuedCount++;
        case UserAutomationStatus.loading:
          loadingCount++;
        case UserAutomationStatus.success:
          successCount++;
        case UserAutomationStatus.failed:
          failedCount++;
        case UserAutomationStatus.expiredQr:
          expiredQrCount++;
      }
    }

    return (
      queuedCount: queuedCount,
      loadingCount: loadingCount,
      successCount: successCount,
      failedCount: failedCount,
      expiredQrCount: expiredQrCount,
    );
  }
}

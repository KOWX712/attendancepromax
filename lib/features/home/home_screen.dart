import 'package:mmuautoqr/app/providers.dart';
import 'package:mmuautoqr/app/ui_mode.dart';
import 'package:mmuautoqr/features/home/home_stats.dart';
import 'package:mmuautoqr/l10n/app_localizations.dart';
import 'package:mmuautoqr/previews/preview_helpers.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widget_previews.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final appUi = ref.watch(appUiProvider);
    final localizations = AppLocalizations.of(context)!;
    final colorScheme = Theme.of(context).colorScheme;
    final stats = ref.watch(homeStatsProvider);
    final users = ref.watch(usersControllerProvider).asData?.value ?? const [];
    final activeUsers = users.where((user) => user.isActive).length;

    void onOpenScanner() {
      if (activeUsers > 0) {
        context.go('/scanner');
      } else {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(localizations.homeAddActiveUserBeforeScanner)),
        );
        context.go('/users');
      }
    }

    return appUi.buildPageScaffold(
      title: localizations.appTitle,
      floatingActionButton: appUi.isApple
          ? null
          : FloatingActionButton(
              onPressed: onOpenScanner,
              child: const Icon(Icons.qr_code_outlined),
            ),
      child: ListView(
        padding: appUi.pagePadding,
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: appUi.isApple
                ? appUi.buildSection(
                    child: _HomeStatsContent(stats: stats),
                  )
                : Card.filled(
                    color: colorScheme.secondaryContainer,
                    child: Padding(
                      padding: const EdgeInsets.all(20),
                      child: _HomeStatsContent(
                        stats: stats,
                        textColor: colorScheme.onSecondaryContainer,
                      ),
                    ),
                  ),
          ),
          if (appUi.isApple) ...[
            const SizedBox(height: 24),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: SizedBox(
                width: double.infinity,
                child: appUi.buildPrimaryButton(
                  onPressed: onOpenScanner,
                  label: localizations.scannerTitle,
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _HomeStatsContent extends StatelessWidget {
  const _HomeStatsContent({required this.stats, this.textColor});

  final HomeStats stats;
  final Color? textColor;

  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;
    final style = textColor != null ? TextStyle(color: textColor) : null;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          localizations.homeUserStatistics,
          style: Theme.of(context).textTheme.titleLarge?.merge(style),
        ),
        const SizedBox(height: 12),
        Text(localizations.homeTotalUsers(stats.totalUsers), style: style),
        const SizedBox(height: 4),
        Text(localizations.homeActiveUsers(stats.activeUsers), style: style),
      ],
    );
  }
}

@Preview(name: 'Home Material', size: previewPhoneSize)
Widget homeMaterialPreview() => buildScreenPreview(
  uiMode: UiModePreference.android,
  child: const HomeScreen(),
);

@Preview(name: 'Home Cupertino', size: previewPhoneSize)
Widget homeCupertinoPreview() => buildScreenPreview(
  uiMode: UiModePreference.apple,
  child: const HomeScreen(),
);

import 'package:mmuautoqr/app/providers.dart';
import 'package:mmuautoqr/core/models/app_tab.dart';
import 'package:mmuautoqr/features/attendance_automation/attendance_status_screen.dart';
import 'package:mmuautoqr/features/home/home_screen.dart';
import 'package:mmuautoqr/features/scanner/scanner_screen.dart';
import 'package:mmuautoqr/features/settings/settings_screen.dart';
import 'package:mmuautoqr/features/users/users_screen.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

final routerProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/',
    routes: [
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) {
          return AppShell(navigationShell: navigationShell);
        },
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/',
                builder: (context, state) => const HomeScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/users',
                builder: (context, state) => const UsersScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/settings',
                builder: (context, state) => const SettingsScreen(),
              ),
            ],
          ),
        ],
      ),
      GoRoute(
        path: '/scanner',
        builder: (context, state) => const ScannerScreen(),
      ),
      GoRoute(
        path: '/attendance-status',
        builder: (context, state) => const AttendanceStatusScreen(),
      ),
    ],
  );
});

bool? resolveLaunchAutoOpenEnabledAtStartup({
  required bool? currentValue,
  required bool? currentSettingsValue,
}) {
  return currentValue ?? currentSettingsValue;
}

bool shouldAutoOpenQrScannerAtLaunch({
  required bool handledLaunchRedirect,
  required bool? launchAutoOpenEnabledAtStartup,
  required int activeUserCount,
}) {
  return !handledLaunchRedirect &&
      (launchAutoOpenEnabledAtStartup ?? false) &&
      activeUserCount > 0;
}

class AppShell extends ConsumerStatefulWidget {
  const AppShell({required this.navigationShell, super.key});

  final StatefulNavigationShell navigationShell;

  @override
  ConsumerState<AppShell> createState() => _AppShellState();
}

class _AppShellState extends ConsumerState<AppShell> {
  bool? _launchAutoOpenEnabledAtStartup;

  @override
  Widget build(BuildContext context) {
    final appUi = ref.watch(appUiProvider);
    final currentTab = AppTab.values[widget.navigationShell.currentIndex];
    final settings = ref.watch(settingsControllerProvider).asData?.value;
    final users = ref.watch(usersControllerProvider).asData?.value ?? const [];
    final activeUserCount = users.where((user) => user.isActive).length;
    final handledLaunchRedirect = ref.watch(
      launchAutoOpenQrScannerHandledProvider,
    );

    _launchAutoOpenEnabledAtStartup = resolveLaunchAutoOpenEnabledAtStartup(
      currentValue: _launchAutoOpenEnabledAtStartup,
      currentSettingsValue: settings?.autoOpenQrScanner,
    );

    if (shouldAutoOpenQrScannerAtLaunch(
      handledLaunchRedirect: handledLaunchRedirect,
      launchAutoOpenEnabledAtStartup: _launchAutoOpenEnabledAtStartup,
      activeUserCount: activeUserCount,
    )) {
      ref.read(launchAutoOpenQrScannerHandledProvider.notifier).markHandled();
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          context.go('/scanner');
        }
      });
    }

    return appUi.buildShell(
      body: widget.navigationShell,
      animationKey: widget.navigationShell.currentIndex,
      currentTab: currentTab,
      onTabSelected: (tab) {
        widget.navigationShell.goBranch(
          tab.index,
          initialLocation: tab.index == widget.navigationShell.currentIndex,
        );
      },
    );
  }
}

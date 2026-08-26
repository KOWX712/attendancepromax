import 'package:mmuautoqr/app/providers.dart';
import 'package:mmuautoqr/app/router.dart';
import 'package:mmuautoqr/app/ui_mode.dart';
import 'package:mmuautoqr/l10n/app_localizations.dart';
import 'package:dynamic_color/dynamic_color.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class AttendanceProMaxApp extends ConsumerWidget {
  const AttendanceProMaxApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(routerProvider);
    final uiMode = ref.watch(effectiveUiModeProvider);
    return DynamicColorBuilder(
      builder: (lightDynamic, darkDynamic) => MaterialApp.router(
        onGenerateTitle: (context) => AppLocalizations.of(context)!.appTitle,
        debugShowCheckedModeBanner: false,
        localizationsDelegates: const [
          AppLocalizations.delegate,
          GlobalMaterialLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
        ],
        supportedLocales: AppLocalizations.supportedLocales,
        themeMode: ThemeMode.system,
        theme: buildAttendanceProMaxLightTheme(
          uiMode: uiMode,
          lightDynamic: lightDynamic,
        ),
        darkTheme: buildAttendanceProMaxDarkTheme(
          uiMode: uiMode,
          darkDynamic: darkDynamic,
        ),
        builder: (context, child) => AttendanceProMaxAppShell(
          uiMode: uiMode,
          child: child ?? const SizedBox.shrink(),
        ),
        routerConfig: router,
      ),
    );
  }
}

class AttendanceProMaxAppShell extends StatelessWidget {
  const AttendanceProMaxAppShell({
    required this.uiMode,
    required this.child,
    super.key,
  });

  final EffectiveUiMode uiMode;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    if (uiMode != EffectiveUiMode.apple) {
      return child;
    }

    final brightness = MediaQuery.platformBrightnessOf(context);
    return CupertinoTheme(
      data: CupertinoThemeData(
        brightness: brightness,
        scaffoldBackgroundColor: CupertinoColors.systemGroupedBackground,
      ),
      child: child,
    );
  }
}

ThemeData buildAttendanceProMaxLightTheme({
  required EffectiveUiMode uiMode,
  ColorScheme? lightDynamic,
}) {
  return _buildMaterialTheme(
    _resolveColorScheme(
      uiMode: uiMode,
      brightness: Brightness.light,
      dynamicScheme: lightDynamic,
    ),
  );
}

ThemeData buildAttendanceProMaxDarkTheme({
  required EffectiveUiMode uiMode,
  ColorScheme? darkDynamic,
}) {
  return _buildMaterialTheme(
    _resolveColorScheme(
      uiMode: uiMode,
      brightness: Brightness.dark,
      dynamicScheme: darkDynamic,
    ),
  );
}

ColorScheme _resolveColorScheme({
  required EffectiveUiMode uiMode,
  required Brightness brightness,
  ColorScheme? dynamicScheme,
}) {
  const fallbackSeedColor = Color(0xFF6750A4);
  const appleSeedColor = Color(0xFF0066CC);
  final seededScheme = ColorScheme.fromSeed(
    seedColor: uiMode == EffectiveUiMode.apple
        ? appleSeedColor
        : fallbackSeedColor,
    brightness: brightness,
  );

  if (uiMode == EffectiveUiMode.apple) {
    return seededScheme;
  }

  if (dynamicScheme == null) {
    return seededScheme;
  }

  return dynamicScheme.copyWith(
    surface: seededScheme.surface,
    onSurface: seededScheme.onSurface,
    surfaceDim: seededScheme.surfaceDim,
    surfaceBright: seededScheme.surfaceBright,
    surfaceContainerLowest: seededScheme.surfaceContainerLowest,
    surfaceContainerLow: seededScheme.surfaceContainerLow,
    surfaceContainer: seededScheme.surfaceContainer,
    surfaceContainerHigh: seededScheme.surfaceContainerHigh,
    surfaceContainerHighest: seededScheme.surfaceContainerHighest,
    inverseSurface: seededScheme.inverseSurface,
    onInverseSurface: seededScheme.onInverseSurface,
    surfaceTint: seededScheme.surfaceTint,
  );
}

ThemeData _buildMaterialTheme(ColorScheme colorScheme) {
  return ThemeData(
    colorScheme: colorScheme,
    scaffoldBackgroundColor: colorScheme.surface,
    canvasColor: colorScheme.surface,
    useMaterial3: true,
  );
}

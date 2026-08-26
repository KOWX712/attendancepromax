import 'package:mmuautoqr/app/providers.dart';
import 'package:mmuautoqr/app/ui_mode.dart';
import 'package:mmuautoqr/l10n/app_localizations.dart';
import 'package:mmuautoqr/features/scanner/scanner_logic.dart';
import 'package:mmuautoqr/features/attendance_automation/providers/automation_providers.dart';
import 'package:mmuautoqr/previews/preview_helpers.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widget_previews.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

class ScannerScreen extends ConsumerStatefulWidget {
  const ScannerScreen({this.previewMode = false, super.key});

  final bool previewMode;

  @override
  ConsumerState<ScannerScreen> createState() => _ScannerScreenState();
}

class _ScannerScreenState extends ConsumerState<ScannerScreen>
    with WidgetsBindingObserver {
  late final MobileScannerController _controller;
  InvalidScanState? _invalidScanState;
  bool _hasNavigated = false;
  double _zoomFactor = 0.0;
  double _baseZoomFactor = 0.0;

  @override
  void initState() {
    super.initState();
    if (widget.previewMode) {
      _controller = MobileScannerController(
        autoStart: false,
        autoZoom: false,
        formats: const [BarcodeFormat.qrCode],
      );
      return;
    }

    WidgetsBinding.instance.addObserver(this);
    _controller = MobileScannerController(
      autoStart: true,
      autoZoom: true,
      formats: const [BarcodeFormat.qrCode],
      detectionSpeed: DetectionSpeed.noDuplicates,
    );
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (!_controller.value.hasCameraPermission) {
      return;
    }

    switch (state) {
      case AppLifecycleState.resumed:
        _controller.start();
        break;
      case AppLifecycleState.inactive:
        _controller.stop();
        break;
      case AppLifecycleState.paused:
      case AppLifecycleState.hidden:
      case AppLifecycleState.detached:
        break;
    }
  }

  @override
  void dispose() {
    if (!widget.previewMode) {
      WidgetsBinding.instance.removeObserver(this);
    }
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final appUi = ref.watch(appUiProvider);
    final localizations = AppLocalizations.of(context)!;
    final activeUsers =
        (ref.watch(usersControllerProvider).asData?.value ?? const [])
            .where((user) => user.isActive)
            .toList(growable: false);

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        if (!didPop && mounted) {
          context.go('/');
        }
      },
      child: appUi.buildPageScaffold(
        title: localizations.scannerTitle,
        onBack: () => context.go('/'),
        child: activeUsers.isEmpty
            ? Center(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Text(
                    localizations.scannerNoActiveUsers,
                    textAlign: TextAlign.center,
                    style: Theme.of(context).textTheme.bodyLarge,
                  ),
                ),
              )
            : widget.previewMode
            ? _PreviewScannerBody()
            : GestureDetector(
                behavior: HitTestBehavior.opaque,
                onScaleStart: (details) {
                  _baseZoomFactor = _zoomFactor;
                },
                onScaleUpdate: (details) {
                  setState(() {
                    _zoomFactor = computeZoomFactorFromGesture(
                      baseZoomFactor: _baseZoomFactor,
                      gestureScale: details.scale,
                    );
                  });
                  _controller.setZoomScale(_zoomFactor);
                },
                child: Stack(
                  children: [
                    MobileScanner(
                      controller: _controller,
                      onDetect: _handleDetect,
                      placeholderBuilder: (context) {
                        return const Center(child: CircularProgressIndicator());
                      },
                      errorBuilder: (context, error) {
                        final localizations = AppLocalizations.of(context)!;
                        return Center(
                          child: Padding(
                            padding: const EdgeInsets.all(24),
                            child: Text(
                              error.errorDetails?.message ??
                                  localizations.scannerCameraStartError,
                              textAlign: TextAlign.center,
                            ),
                          ),
                        );
                      },
                    ),
                    // Dimmed overlay with cutout
                    ColorFiltered(
                      colorFilter: ColorFilter.mode(
                        Colors.black.withAlpha(127),
                        BlendMode.srcOut,
                      ),
                      child: Stack(
                        children: [
                          Container(
                            decoration: const BoxDecoration(
                              color: Colors.black,
                              backgroundBlendMode: BlendMode.dstOut,
                            ),
                          ),
                          Align(
                            alignment: Alignment.center,
                            child: Container(
                              width: 250,
                              height: 250,
                              decoration: BoxDecoration(
                                color: Colors.white,
                                borderRadius: BorderRadius.circular(24),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    // Border for the cutout
                    Center(
                      child: Container(
                        width: 250,
                        height: 250,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(24),
                          border: Border.all(
                            color: Theme.of(context).colorScheme.primary,
                            width: 3,
                          ),
                        ),
                      ),
                    ),
                    Align(
                      alignment: Alignment.bottomCenter,
                      child: Padding(
                        padding: const EdgeInsets.all(16),
                        child: DecoratedBox(
                          decoration: BoxDecoration(
                            color: Colors.black.withAlpha(178),
                            borderRadius: BorderRadius.circular(16),
                          ),
                          child: Padding(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 12,
                            ),
                            child: Text(
                              localizations.scannerHint,
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 13,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
      ),
    );
  }

  void _handleDetect(BarcodeCapture capture) {
    if (_hasNavigated) {
      return;
    }
    final localizations = AppLocalizations.of(context)!;

    for (final barcode in capture.barcodes) {
      final rawValue = barcode.rawValue?.trim();
      if (rawValue == null || rawValue.isEmpty) {
        continue;
      }

      if (isValidAttendanceUrl(rawValue)) {
        final activeUsers =
            (ref.read(usersControllerProvider).asData?.value ?? const [])
                .where((user) => user.isActive)
                .toList(growable: false);

        if (activeUsers.isEmpty) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(localizations.scannerNoActiveUsers),
            ),
          );
          return;
        }

        _hasNavigated = true;
        _controller.stop();

        // Start automation in background and navigate to status screen
        ref.read(automationOrchestratorProvider.future).then((orchestrator) {
          orchestrator.startAutomation(qrUrl: rawValue, users: activeUsers);
        });

        if (mounted) {
          context.go('/attendance-status');
        }
        return;
      }

      final nowMillis = DateTime.now().millisecondsSinceEpoch;
      final shouldSuppressMessage = shouldThrottleInvalidScan(
        rawValue: rawValue,
        previousState: _invalidScanState,
        nowMillis: nowMillis,
      );
      _invalidScanState = InvalidScanState(
        rawValue: rawValue,
        timestampMillis: nowMillis,
      );

      if (!shouldSuppressMessage && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(localizations.scannerInvalidAttendanceUrl)),
        );
      }
      return;
    }
  }
}

class _PreviewScannerBody extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final localizations = AppLocalizations.of(context)!;
    final colorScheme = Theme.of(context).colorScheme;
    return Stack(
      children: [
        Positioned.fill(
          child: DecoratedBox(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [
                  colorScheme.surfaceContainerHighest,
                  colorScheme.surfaceContainer,
                ],
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
              ),
            ),
          ),
        ),
        Center(
          child: Container(
            width: 220,
            height: 220,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(24),
              border: Border.all(color: colorScheme.primary, width: 3),
            ),
          ),
        ),
        Align(
          alignment: Alignment.bottomCenter,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: Colors.black.withAlpha(178),
                borderRadius: BorderRadius.circular(16),
              ),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                child: Text(
                  localizations.scannerHint,
                  style: const TextStyle(color: Colors.white, fontSize: 13),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

@Preview(name: 'Scanner Material', size: previewPhoneSize)
Widget scannerMaterialPreview() => buildScreenPreview(
  uiMode: UiModePreference.android,
  child: const ScannerScreen(previewMode: true),
);

@Preview(name: 'Scanner Cupertino', size: previewPhoneSize)
Widget scannerCupertinoPreview() => buildScreenPreview(
  uiMode: UiModePreference.apple,
  child: const ScannerScreen(previewMode: true),
);

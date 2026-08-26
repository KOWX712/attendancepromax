const invalidScanCooldownMillis = 2000;
const pinchZoomSensitivity = 0.5;

class InvalidScanState {
  const InvalidScanState({
    required this.rawValue,
    required this.timestampMillis,
  });

  final String rawValue;
  final int timestampMillis;
}

bool isValidAttendanceUrl(String value) {
  final uri = Uri.tryParse(value.trim());
  if (uri == null) {
    return false;
  }

  return (uri.scheme == 'http' || uri.scheme == 'https') && uri.host.isNotEmpty;
}

bool shouldThrottleInvalidScan({
  required String rawValue,
  required InvalidScanState? previousState,
  required int nowMillis,
  int cooldownMillis = invalidScanCooldownMillis,
}) {
  if (previousState == null) {
    return false;
  }

  return previousState.rawValue == rawValue &&
      nowMillis - previousState.timestampMillis < cooldownMillis;
}

double computeZoomFactorFromGesture({
  required double baseZoomFactor,
  required double gestureScale,
  double sensitivity = pinchZoomSensitivity,
}) {
  return (baseZoomFactor + ((gestureScale - 1.0) * sensitivity)).clamp(
    0.0,
    1.0,
  );
}

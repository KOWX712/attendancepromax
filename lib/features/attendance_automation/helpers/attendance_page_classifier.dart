/// Page classification helpers for Oracle SSO attendance automation.
/// 
/// Detects various page states during the attendance QR scan and form submission flow.
/// These contracts encode Oracle's page redirection quirks and must remain stable
/// for headless workers (flutter_inappwebview) to reuse the same detection logic.
class AttendancePageClassifier {
  const AttendancePageClassifier();

  /// String markers for page state detection.
  static const String _intermediateLoginQueryMarker = 'cmd=login';
  static const String _intermediateLoginErrorMarker = 'errorCode=105';
  static const String _expiredQrMessageMarker = 'Please signin from proper QR Code URL';
  static const String _attendanceLoginUserIdField = 'N_QRCODE_DRV_USERID';
  static const String _attendanceLoginPasswordField = 'N_QRCODE_DRV_PASSWORD';

  /// Detects if the rendered page shows an expired QR code error.
  /// 
  /// The Oracle attendance form returns a specific message when the QR code
  /// has expired or was scanned from an invalid source.
  /// 
  /// Contract: This method encodes the exact error message Oracle returns.
  /// Do not modify the string constant without verifying on a live Oracle instance.
  bool isExpiredQrPage({required String renderedHtml}) {
    return renderedHtml.toLowerCase().contains(
      _expiredQrMessageMarker.toLowerCase(),
    );
  }

  /// Detects the intermediate Oracle SSO bootstrap page during attendance flow.
  /// 
  /// The Oracle sign-in bootstrap page redirects itself to the attendance form.
  /// It is not a failed QR scan and must stay open until that navigation finishes.
  /// This method returns true only if we are on that intermediate page and the
  /// attendance login form is NOT yet present.
  /// 
  /// Contract: Checks for cmd=login and errorCode=105 in the rendered URL,
  /// then verifies the attendance form fields are absent.
  bool isIntermediateAttendanceLoginPage({
    required String requestedUrl,
    required String? renderedUrl,
    required String renderedHtml,
  }) {
    return _isIntermediateAttendanceLoginUrl(
          requestedUrl: requestedUrl,
          renderedUrl: renderedUrl,
        ) &&
        !_hasAttendanceLoginForm(renderedHtml);
  }

  /// Determines if we should retry the intermediate attendance page once.
  /// 
  /// Oracle's bootstrap page sometimes loads blank from cache. We retry exactly once
  /// by clearing the cache and reloading the QR URL.
  /// 
  /// Contract: Returns true only on the first encounter if the URL matches
  /// intermediate markers. Subsequent encounters return false (hasRetriedIntermediatePage is true).
  bool shouldRetryIntermediateAttendancePage({
    required String requestedUrl,
    required String? renderedUrl,
    required bool hasRetriedIntermediatePage,
  }) {
    if (hasRetriedIntermediatePage) {
      return false;
    }

    return _isIntermediateAttendanceLoginUrl(
      requestedUrl: requestedUrl,
      renderedUrl: renderedUrl,
    );
  }

  /// Checks if the rendered HTML contains the attendance login form fields.
  /// 
  /// The attendance form contains specific input field names that identify it uniquely.
  bool _hasAttendanceLoginForm(String renderedHtml) {
    return renderedHtml.contains(_attendanceLoginUserIdField) &&
        renderedHtml.contains(_attendanceLoginPasswordField);
  }

  /// Checks if the current URL matches the intermediate attendance login redirect pattern.
  /// 
  /// The intermediate page URL contains both cmd=login and errorCode=105 markers,
  /// and the rendered URL must start with the originally requested URL (case-insensitive).
  bool _isIntermediateAttendanceLoginUrl({
    required String requestedUrl,
    required String? renderedUrl,
  }) {
    if (requestedUrl.isEmpty) {
      return false;
    }

    final normalizedUrl = renderedUrl ?? '';
    final lowerUrl = normalizedUrl.toLowerCase();
    final hasLoginRedirectMarkers =
        lowerUrl.contains(_intermediateLoginQueryMarker.toLowerCase()) &&
        lowerUrl.contains(_intermediateLoginErrorMarker.toLowerCase());

    if (!hasLoginRedirectMarkers) {
      return false;
    }

    return normalizedUrl.toLowerCase().startsWith(requestedUrl.toLowerCase());
  }
}

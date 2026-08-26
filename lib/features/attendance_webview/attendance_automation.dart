const automationUserIdPlaceholder = '__USER_ID__';
const automationPasswordPlaceholder = '__PASSWORD__';
const automationRunIdPlaceholder = '__RUN_ID__';

const _intermediateLoginQueryMarker = 'cmd=login';
const _intermediateLoginErrorMarker = 'errorCode=105';
const _expiredQrMessageMarker = 'Please signin from proper QR Code URL';
const _attendanceLoginUserIdField = 'N_QRCODE_DRV_USERID';
const _attendanceLoginPasswordField = 'N_QRCODE_DRV_PASSWORD';

String renderAutomationScriptTemplate({
  required String template,
  required String userId,
  required String password,
  required int automationRunId,
}) {
  return template
      .replaceAll(
        automationUserIdPlaceholder,
        _escapeForSingleQuotedJsString(userId),
      )
      .replaceAll(
        automationPasswordPlaceholder,
        _escapeForSingleQuotedJsString(password),
      )
      .replaceAll(automationRunIdPlaceholder, automationRunId.toString());
}

bool shouldReopenQrScannerForAttendancePage({
  required String requestedUrl,
  required String? renderedUrl,
  required String renderedHtml,
}) {
  if (requestedUrl.isEmpty || renderedHtml.isEmpty) {
    return false;
  }

  return _isExpiredAttendancePage(renderedHtml);
}

/// The Oracle sign-in bootstrap page redirects itself to the attendance form.
/// It is not a failed QR scan and must stay open until that navigation finishes.
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

String _escapeForSingleQuotedJsString(String value) {
  return value.replaceAll(r'\', r'\\').replaceAll("'", r"\'");
}

bool _hasAttendanceLoginForm(String renderedHtml) {
  return renderedHtml.contains(_attendanceLoginUserIdField) &&
      renderedHtml.contains(_attendanceLoginPasswordField);
}

bool _isExpiredAttendancePage(String renderedHtml) {
  return renderedHtml.toLowerCase().contains(
    _expiredQrMessageMarker.toLowerCase(),
  );
}

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

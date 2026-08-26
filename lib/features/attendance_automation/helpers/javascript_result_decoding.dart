import 'dart:convert';

String decodeJavaScriptHtmlResult(Object? result) {
  if (result == null) {
    return '';
  }

  if (result is String) {
    final trimmed = result.trim();
    if (trimmed == 'null') {
      return '';
    }
    if (trimmed.startsWith('"') && trimmed.endsWith('"')) {
      try {
        return jsonStringToString(trimmed);
      } catch (_) {
        return trimmed;
      }
    }
    return trimmed;
  }

  return result.toString();
}

String jsonStringToString(String value) {
  return jsonDecode(value) as String;
}

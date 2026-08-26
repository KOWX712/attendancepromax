// ignore: unused_import
import 'package:intl/intl.dart' as intl;

import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for English (`en`).
class AppLocalizationsEn extends AppLocalizations {
  AppLocalizationsEn([String locale = 'en']) : super(locale);

  @override
  String get appTitle => 'AttendanceProMax';

  @override
  String get tabHome => 'Home';

  @override
  String get tabUsers => 'Users';

  @override
  String get tabSettings => 'Settings';

  @override
  String get homeUserStatistics => 'User statistics';

  @override
  String homeTotalUsers(int count) {
    return 'Total users: $count';
  }

  @override
  String homeActiveUsers(int count) {
    return 'Active users: $count';
  }

  @override
  String get homeAddActiveUserBeforeScanner =>
      'Add an active user before opening the scanner.';

  @override
  String get settingsTitle => 'Settings';

  @override
  String get settingsUiMode => 'UI mode';

  @override
  String get settingsUiModeApple => 'Apple';

  @override
  String get settingsUiModeAndroid => 'Android';

  @override
  String get settingsAutoOpenQrScannerTitle => 'Open QR scanner on app launch';

  @override
  String get settingsAutoOpenQrScannerSubtitle =>
      'Only if any active user exist.';

  @override
  String get usersTitle => 'Users';

  @override
  String get usersDuplicateIdError => 'User ID already exists.';

  @override
  String get usersNameLabel => 'Name';

  @override
  String get usersIdLabel => 'User ID';

  @override
  String get usersPasswordLabel => 'Password';

  @override
  String get usersAddTitle => 'Add user';

  @override
  String get usersEditTitle => 'Edit user';

  @override
  String get commonCancel => 'Cancel';

  @override
  String get commonDelete => 'Delete';

  @override
  String get commonAdd => 'Add';

  @override
  String get commonSave => 'Save';

  @override
  String get usersValidationNameRequired => 'Name is required.';

  @override
  String get usersValidationIdTooShort =>
      'User ID must be at least 3 characters.';

  @override
  String get usersValidationPasswordTooShort =>
      'Password must be at least 4 characters.';

  @override
  String get usersShowPassword => 'Show password';

  @override
  String get usersHidePassword => 'Hide password';

  @override
  String get scannerTitle => 'Scan QR code';

  @override
  String get scannerNoActiveUsers =>
      'Add at least one active user before starting the attendance scanner.';

  @override
  String get scannerCameraStartError => 'Unable to start camera scanner.';

  @override
  String get scannerHint => 'Point the camera at the attendance QR code.';

  @override
  String get scannerInvalidAttendanceUrl =>
      'Scanned QR code is not a valid attendance URL.';

  @override
  String get attendanceTitle => 'Attendance';

  @override
  String get attendanceLoadingStatus => 'Loading attendance page...';

  @override
  String attendanceSubmittingStatus(String name) {
    return 'Submitting attendance for $name...';
  }

  @override
  String get attendanceNoActiveUsers =>
      'No active users found. Activate at least one user and scan again.';

  @override
  String get attendanceRetry => 'Retry';

  @override
  String attendanceLoginSubmittedStatus(String name) {
    return 'Login submitted for $name';
  }

  @override
  String get attendanceUnknownLoginError => 'Unknown login error';

  @override
  String attendanceLoginFailed(String reason) {
    return 'Login failed: $reason';
  }

  @override
  String get attendanceCompleted => 'Attendance automation completed.';

  @override
  String get attendancePreviewHost => 'attendance.example.com';

  @override
  String get attendancePreviewDescription =>
      'Attendance page preview\n\nWebView content is replaced with a static mock in the widget previewer.';
}

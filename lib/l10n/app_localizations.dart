import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:intl/intl.dart' as intl;

import 'app_localizations_en.dart';

// ignore_for_file: type=lint

/// Callers can lookup localized strings with an instance of AppLocalizations
/// returned by `AppLocalizations.of(context)`.
///
/// Applications need to include `AppLocalizations.delegate()` in their app's
/// `localizationDelegates` list, and the locales they support in the app's
/// `supportedLocales` list. For example:
///
/// ```dart
/// import 'l10n/app_localizations.dart';
///
/// return MaterialApp(
///   localizationsDelegates: AppLocalizations.localizationsDelegates,
///   supportedLocales: AppLocalizations.supportedLocales,
///   home: MyApplicationHome(),
/// );
/// ```
///
/// ## Update pubspec.yaml
///
/// Please make sure to update your pubspec.yaml to include the following
/// packages:
///
/// ```yaml
/// dependencies:
///   # Internationalization support.
///   flutter_localizations:
///     sdk: flutter
///   intl: any # Use the pinned version from flutter_localizations
///
///   # Rest of dependencies
/// ```
///
/// ## iOS Applications
///
/// iOS applications define key application metadata, including supported
/// locales, in an Info.plist file that is built into the application bundle.
/// To configure the locales supported by your app, you’ll need to edit this
/// file.
///
/// First, open your project’s ios/Runner.xcworkspace Xcode workspace file.
/// Then, in the Project Navigator, open the Info.plist file under the Runner
/// project’s Runner folder.
///
/// Next, select the Information Property List item, select Add Item from the
/// Editor menu, then select Localizations from the pop-up menu.
///
/// Select and expand the newly-created Localizations item then, for each
/// locale your application supports, add a new item and select the locale
/// you wish to add from the pop-up menu in the Value field. This list should
/// be consistent with the languages listed in the AppLocalizations.supportedLocales
/// property.
abstract class AppLocalizations {
  AppLocalizations(String locale)
    : localeName = intl.Intl.canonicalizedLocale(locale.toString());

  final String localeName;

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  /// A list of this localizations delegate along with the default localizations
  /// delegates.
  ///
  /// Returns a list of localizations delegates containing this delegate along with
  /// GlobalMaterialLocalizations.delegate, GlobalCupertinoLocalizations.delegate,
  /// and GlobalWidgetsLocalizations.delegate.
  ///
  /// Additional delegates can be added by appending to this list in
  /// MaterialApp. This list does not have to be used at all if a custom list
  /// of delegates is preferred or required.
  static const List<LocalizationsDelegate<dynamic>> localizationsDelegates =
      <LocalizationsDelegate<dynamic>>[
        delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
      ];

  /// A list of this localizations delegate's supported locales.
  static const List<Locale> supportedLocales = <Locale>[Locale('en')];

  /// No description provided for @appTitle.
  ///
  /// In en, this message translates to:
  /// **'AttendanceProMax'**
  String get appTitle;

  /// No description provided for @tabHome.
  ///
  /// In en, this message translates to:
  /// **'Home'**
  String get tabHome;

  /// No description provided for @tabUsers.
  ///
  /// In en, this message translates to:
  /// **'Users'**
  String get tabUsers;

  /// No description provided for @tabSettings.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get tabSettings;

  /// No description provided for @homeUserStatistics.
  ///
  /// In en, this message translates to:
  /// **'User statistics'**
  String get homeUserStatistics;

  /// No description provided for @homeTotalUsers.
  ///
  /// In en, this message translates to:
  /// **'Total users: {count}'**
  String homeTotalUsers(int count);

  /// No description provided for @homeActiveUsers.
  ///
  /// In en, this message translates to:
  /// **'Active users: {count}'**
  String homeActiveUsers(int count);

  /// No description provided for @homeAddActiveUserBeforeScanner.
  ///
  /// In en, this message translates to:
  /// **'Add an active user before opening the scanner.'**
  String get homeAddActiveUserBeforeScanner;

  /// No description provided for @settingsTitle.
  ///
  /// In en, this message translates to:
  /// **'Settings'**
  String get settingsTitle;

  /// No description provided for @settingsUiMode.
  ///
  /// In en, this message translates to:
  /// **'UI mode'**
  String get settingsUiMode;

  /// No description provided for @settingsUiModeApple.
  ///
  /// In en, this message translates to:
  /// **'Apple'**
  String get settingsUiModeApple;

  /// No description provided for @settingsUiModeAndroid.
  ///
  /// In en, this message translates to:
  /// **'Android'**
  String get settingsUiModeAndroid;

  /// No description provided for @settingsAutoOpenQrScannerTitle.
  ///
  /// In en, this message translates to:
  /// **'Open QR scanner on app launch'**
  String get settingsAutoOpenQrScannerTitle;

  /// No description provided for @settingsAutoOpenQrScannerSubtitle.
  ///
  /// In en, this message translates to:
  /// **'Only if any active user exist.'**
  String get settingsAutoOpenQrScannerSubtitle;

  /// No description provided for @usersTitle.
  ///
  /// In en, this message translates to:
  /// **'Users'**
  String get usersTitle;

  /// No description provided for @usersDuplicateIdError.
  ///
  /// In en, this message translates to:
  /// **'User ID already exists.'**
  String get usersDuplicateIdError;

  /// No description provided for @usersNameLabel.
  ///
  /// In en, this message translates to:
  /// **'Name'**
  String get usersNameLabel;

  /// No description provided for @usersIdLabel.
  ///
  /// In en, this message translates to:
  /// **'User ID'**
  String get usersIdLabel;

  /// No description provided for @usersPasswordLabel.
  ///
  /// In en, this message translates to:
  /// **'Password'**
  String get usersPasswordLabel;

  /// No description provided for @usersAddTitle.
  ///
  /// In en, this message translates to:
  /// **'Add user'**
  String get usersAddTitle;

  /// No description provided for @usersEditTitle.
  ///
  /// In en, this message translates to:
  /// **'Edit user'**
  String get usersEditTitle;

  /// No description provided for @commonCancel.
  ///
  /// In en, this message translates to:
  /// **'Cancel'**
  String get commonCancel;

  /// No description provided for @commonDelete.
  ///
  /// In en, this message translates to:
  /// **'Delete'**
  String get commonDelete;

  /// No description provided for @commonAdd.
  ///
  /// In en, this message translates to:
  /// **'Add'**
  String get commonAdd;

  /// No description provided for @commonSave.
  ///
  /// In en, this message translates to:
  /// **'Save'**
  String get commonSave;

  /// No description provided for @usersValidationNameRequired.
  ///
  /// In en, this message translates to:
  /// **'Name is required.'**
  String get usersValidationNameRequired;

  /// No description provided for @usersValidationIdTooShort.
  ///
  /// In en, this message translates to:
  /// **'User ID must be at least 3 characters.'**
  String get usersValidationIdTooShort;

  /// No description provided for @usersValidationPasswordTooShort.
  ///
  /// In en, this message translates to:
  /// **'Password must be at least 4 characters.'**
  String get usersValidationPasswordTooShort;

  /// No description provided for @usersShowPassword.
  ///
  /// In en, this message translates to:
  /// **'Show password'**
  String get usersShowPassword;

  /// No description provided for @usersHidePassword.
  ///
  /// In en, this message translates to:
  /// **'Hide password'**
  String get usersHidePassword;

  /// No description provided for @scannerTitle.
  ///
  /// In en, this message translates to:
  /// **'Scan QR code'**
  String get scannerTitle;

  /// No description provided for @scannerNoActiveUsers.
  ///
  /// In en, this message translates to:
  /// **'Add at least one active user before starting the attendance scanner.'**
  String get scannerNoActiveUsers;

  /// No description provided for @scannerCameraStartError.
  ///
  /// In en, this message translates to:
  /// **'Unable to start camera scanner.'**
  String get scannerCameraStartError;

  /// No description provided for @scannerHint.
  ///
  /// In en, this message translates to:
  /// **'Point the camera at the attendance QR code.'**
  String get scannerHint;

  /// No description provided for @scannerInvalidAttendanceUrl.
  ///
  /// In en, this message translates to:
  /// **'Scanned QR code is not a valid attendance URL.'**
  String get scannerInvalidAttendanceUrl;

  /// No description provided for @attendanceTitle.
  ///
  /// In en, this message translates to:
  /// **'Attendance'**
  String get attendanceTitle;

  /// No description provided for @attendanceLoadingStatus.
  ///
  /// In en, this message translates to:
  /// **'Loading attendance page...'**
  String get attendanceLoadingStatus;

  /// No description provided for @attendanceSubmittingStatus.
  ///
  /// In en, this message translates to:
  /// **'Submitting attendance for {name}...'**
  String attendanceSubmittingStatus(String name);

  /// No description provided for @attendanceNoActiveUsers.
  ///
  /// In en, this message translates to:
  /// **'No active users found. Activate at least one user and scan again.'**
  String get attendanceNoActiveUsers;

  /// No description provided for @attendanceRetry.
  ///
  /// In en, this message translates to:
  /// **'Retry'**
  String get attendanceRetry;

  /// No description provided for @attendanceLoginSubmittedStatus.
  ///
  /// In en, this message translates to:
  /// **'Login submitted for {name}'**
  String attendanceLoginSubmittedStatus(String name);

  /// No description provided for @attendanceUnknownLoginError.
  ///
  /// In en, this message translates to:
  /// **'Unknown login error'**
  String get attendanceUnknownLoginError;

  /// No description provided for @attendanceLoginFailed.
  ///
  /// In en, this message translates to:
  /// **'Login failed: {reason}'**
  String attendanceLoginFailed(String reason);

  /// No description provided for @attendanceCompleted.
  ///
  /// In en, this message translates to:
  /// **'Attendance automation completed.'**
  String get attendanceCompleted;

  /// No description provided for @attendancePreviewHost.
  ///
  /// In en, this message translates to:
  /// **'attendance.example.com'**
  String get attendancePreviewHost;

  /// No description provided for @attendancePreviewDescription.
  ///
  /// In en, this message translates to:
  /// **'Attendance page preview\n\nWebView content is replaced with a static mock in the widget previewer.'**
  String get attendancePreviewDescription;
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  Future<AppLocalizations> load(Locale locale) {
    return SynchronousFuture<AppLocalizations>(lookupAppLocalizations(locale));
  }

  @override
  bool isSupported(Locale locale) =>
      <String>['en'].contains(locale.languageCode);

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}

AppLocalizations lookupAppLocalizations(Locale locale) {
  // Lookup logic when only language code is specified.
  switch (locale.languageCode) {
    case 'en':
      return AppLocalizationsEn();
  }

  throw FlutterError(
    'AppLocalizations.delegate failed to load unsupported locale "$locale". This is likely '
    'an issue with the localizations generation tool. Please file an issue '
    'on GitHub with a reproducible sample app and the gen-l10n configuration '
    'that was used.',
  );
}

import 'package:mmuautoqr/l10n/app_localizations.dart';

String? validateUserName(String value, AppLocalizations localizations) {
  if (value.trim().isEmpty) {
    return localizations.usersValidationNameRequired;
  }

  return null;
}

String? validateUserId(String value, AppLocalizations localizations) {
  if (value.trim().length < 3) {
    return localizations.usersValidationIdTooShort;
  }

  return null;
}

String? validatePasswordForCreate(
  String value,
  AppLocalizations localizations,
) {
  if (value.trim().length < 4) {
    return localizations.usersValidationPasswordTooShort;
  }

  return null;
}

String? validatePasswordForUpdate(
  String value,
  AppLocalizations localizations,
) {
  if (value.trim().isEmpty) {
    return null;
  }

  if (value.trim().length < 4) {
    return localizations.usersValidationPasswordTooShort;
  }

  return null;
}

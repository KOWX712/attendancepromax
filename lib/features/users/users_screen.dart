import 'package:mmuautoqr/app/providers.dart';
import 'package:mmuautoqr/app/ui_mode.dart';
import 'package:mmuautoqr/core/models/user_record.dart';
import 'package:mmuautoqr/core/repositories/user_repository.dart';
import 'package:mmuautoqr/features/users/user_form_validator.dart';
import 'package:mmuautoqr/l10n/app_localizations.dart';
import 'package:mmuautoqr/previews/preview_helpers.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/widget_previews.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

class UsersScreen extends ConsumerWidget {
  const UsersScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final appUi = ref.watch(appUiProvider);
    final localizations = AppLocalizations.of(context)!;
    final usersState = ref.watch(usersControllerProvider);
    final users = usersState.asData?.value ?? <UserRecord>[];

    return appUi.buildPageScaffold(
      title: localizations.usersTitle,
      trailing: appUi.isApple
          ? appUi.buildIconAction(
              materialIcon: Icons.add,
              cupertinoIcon: CupertinoIcons.add,
              onPressed: () => _showUserDialog(context, ref),
            )
          : null,
      floatingActionButton: appUi.isApple
          ? null
          : FloatingActionButton(
              onPressed: () => _showUserDialog(context, ref),
              child: const Icon(Icons.add),
            ),
      child: usersState.isLoading
          ? const Center(child: CircularProgressIndicator())
          : ListView.builder(
              padding: EdgeInsets.only(top: appUi.isApple ? 8 : 16),
              itemCount: users.length,
              itemBuilder: (context, index) {
                final user = users[index];
                return appUi.buildListRow(
                  title: user.name,
                  subtitle: user.userId,
                  showDivider: index > 0,
                  onTap: () => _showUserDialog(context, ref, initialUser: user),
                  trailing: appUi.isApple
                      ? CupertinoSwitch(
                          value: user.isActive,
                          onChanged: (value) {
                            ref
                                .read(usersControllerProvider.notifier)
                                .updateUser(user.copyWith(isActive: value));
                          },
                        )
                      : Switch(
                          value: user.isActive,
                          onChanged: (value) {
                            ref
                                .read(usersControllerProvider.notifier)
                                .updateUser(user.copyWith(isActive: value));
                          },
                        ),
                );
              },
            ),
    );
  }

  Future<void> _showUserDialog(
    BuildContext context,
    WidgetRef ref, {
    UserRecord? initialUser,
  }) async {
    final appUi = ref.read(appUiProvider);
    final localizations = AppLocalizations.of(context)!;
    final nameController = TextEditingController(text: initialUser?.name ?? '');
    final userIdController = TextEditingController(
      text: initialUser?.userId ?? '',
    );
    final passwordController = TextEditingController();
    String? errorText;

    Future<void> submit(StateSetter setState, VoidCallback closeDialog) async {
      final nameError = validateUserName(nameController.text, localizations);
      final userIdError = initialUser == null
          ? validateUserId(userIdController.text, localizations)
          : null;
      final passwordError = initialUser == null
          ? validatePasswordForCreate(passwordController.text, localizations)
          : validatePasswordForUpdate(passwordController.text, localizations);

      errorText = nameError ?? userIdError ?? passwordError;
      if (errorText != null) {
        setState(() {});
        return;
      }

      try {
        if (initialUser == null) {
          await ref
              .read(usersControllerProvider.notifier)
              .addUser(
                name: nameController.text,
                userId: userIdController.text,
                password: passwordController.text,
              );
        } else {
          await ref
              .read(usersControllerProvider.notifier)
              .updateUser(
                initialUser.copyWith(
                  name: nameController.text,
                  password: passwordController.text,
                ),
              );
        }

        closeDialog();
      } on DuplicateUserIdException {
        errorText = localizations.usersDuplicateIdError;
        setState(() {});
      }
    }

    final dialog = StatefulBuilder(
      builder: (context, setState) {
        final content = Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            appUi.buildTextField(
              controller: nameController,
              label: localizations.usersNameLabel,
            ),
            const SizedBox(height: 12),
            appUi.buildTextField(
              controller: userIdController,
              label: localizations.usersIdLabel,
              enabled: initialUser == null,
            ),
            const SizedBox(height: 12),
            appUi.buildTextField(
              controller: passwordController,
              label: localizations.usersPasswordLabel,
              obscureText: true,
              showPasswordVisibilityToggle: true,
              autofillHints: appUi.isApple
                  ? null
                  : [AutofillHints.newPassword, AutofillHints.password],
            ),
            if (errorText != null) ...[
              const SizedBox(height: 12),
              Text(errorText!, style: const TextStyle(color: Colors.red)),
            ],
          ],
        );

        if (appUi.isApple) {
          return CupertinoAlertDialog(
            title: Text(
              initialUser == null
                  ? localizations.usersAddTitle
                  : localizations.usersEditTitle,
            ),
            content: Padding(
              padding: const EdgeInsets.only(top: 12),
              child: content,
            ),
            actions: [
              CupertinoDialogAction(
                onPressed: () => Navigator.of(context).pop(),
                child: Text(localizations.commonCancel),
              ),
              if (initialUser != null)
                CupertinoDialogAction(
                  isDestructiveAction: true,
                  onPressed: () async {
                    await ref
                        .read(usersControllerProvider.notifier)
                        .deleteUser(initialUser.id);
                    if (context.mounted) {
                      Navigator.of(context).pop();
                    }
                  },
                  child: Text(localizations.commonDelete),
                ),
              CupertinoDialogAction(
                isDefaultAction: true,
                onPressed: () =>
                    submit(setState, () => Navigator.of(context).pop()),
                child: Text(
                  initialUser == null
                      ? localizations.commonAdd
                      : localizations.commonSave,
                ),
              ),
            ],
          );
        }

        return AlertDialog(
          title: Text(
            initialUser == null
                ? localizations.usersAddTitle
                : localizations.usersEditTitle,
          ),
          content: content,
          actions: [
            if (initialUser != null)
              TextButton(
                onPressed: () async {
                  await ref
                      .read(usersControllerProvider.notifier)
                      .deleteUser(initialUser.id);
                  if (context.mounted) {
                    Navigator.of(context).pop();
                  }
                },
                child: Text(localizations.commonDelete),
              ),
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: Text(localizations.commonCancel),
            ),
            FilledButton(
              onPressed: () =>
                  submit(setState, () => Navigator.of(context).pop()),
              child: Text(
                initialUser == null
                    ? localizations.commonAdd
                    : localizations.commonSave,
              ),
            ),
          ],
        );
      },
    );

    if (appUi.isApple) {
      await showCupertinoDialog<void>(context: context, builder: (_) => dialog);
    } else {
      await showDialog<void>(context: context, builder: (_) => dialog);
    }
  }
}

@Preview(name: 'Users Material', size: previewPhoneSize)
Widget usersMaterialPreview() => buildScreenPreview(
  uiMode: UiModePreference.android,
  child: const UsersScreen(),
);

@Preview(name: 'Users Cupertino', size: previewPhoneSize)
Widget usersCupertinoPreview() => buildScreenPreview(
  uiMode: UiModePreference.apple,
  child: const UsersScreen(),
);

import 'package:mmuautoqr/app/ui_mode.dart';
import 'package:mmuautoqr/core/models/app_tab.dart';
import 'package:mmuautoqr/l10n/app_localizations.dart';
import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

class AppUi {
  const AppUi(this.mode);

  final EffectiveUiMode mode;

  bool get isApple => mode == EffectiveUiMode.apple;
  EdgeInsets get pagePadding => isApple
      ? const EdgeInsets.symmetric(vertical: 12)
      : const EdgeInsets.symmetric(vertical: 16);

  Widget buildShell({
    required Widget body,
    required Object animationKey,
    required AppTab currentTab,
    required ValueChanged<AppTab> onTabSelected,
  }) {
    return Builder(
      builder: (context) => Scaffold(
        body: AnimatedSwitcher(
          duration: const Duration(milliseconds: 170),
          switchInCurve: Curves.easeInOut,
          switchOutCurve: Curves.easeInOut,
          child: KeyedSubtree(
            key: ValueKey(animationKey),
            child: body,
          ),
        ),
        bottomNavigationBar: buildTabBar(
          context,
          currentTab: currentTab,
          onTabSelected: onTabSelected,
        ),
      ),
    );
  }

  Widget buildPageScaffold({
    required String title,
    required Widget child,
    Widget? trailing,
    Widget? floatingActionButton,
    VoidCallback? onBack,
  }) {
    if (isApple) {
      return Builder(
        builder: (context) {
          final cupertinoTheme = CupertinoTheme.of(context);
          return CupertinoPageScaffold(
            backgroundColor: cupertinoTheme.scaffoldBackgroundColor,
            navigationBar: CupertinoNavigationBar(
              middle: Text(title),
              leading: onBack == null
                  ? null
                  : CupertinoButton(
                      padding: EdgeInsets.zero,
                      minimumSize: Size.zero,
                      onPressed: onBack,
                      child: const Icon(CupertinoIcons.back),
                    ),
              trailing: trailing,
            ),
            child: SafeArea(bottom: false, child: child),
          );
        },
      );
    }

    return Scaffold(
      appBar: AppBar(
        automaticallyImplyLeading: onBack == null,
        leading: onBack == null
            ? null
            : IconButton(onPressed: onBack, icon: const Icon(Icons.arrow_back)),
        title: Text(title),
        actions: trailing == null ? null : <Widget>[trailing],
      ),
      floatingActionButton: floatingActionButton,
      body: child,
    );
  }

  Widget buildTabBar(
    BuildContext context, {
    required AppTab currentTab,
    required ValueChanged<AppTab> onTabSelected,
  }) {
    final localizations = AppLocalizations.of(context)!;
    final items =
        <({
          AppTab tab,
          String label,
          IconData icon,
          IconData selectedIcon,
          IconData cupertinoIcon,
        })>[
          (
            tab: AppTab.home,
            label: localizations.tabHome,
            icon: Icons.home_outlined,
            selectedIcon: Icons.home,
            cupertinoIcon: CupertinoIcons.house,
          ),
          (
            tab: AppTab.users,
            label: localizations.tabUsers,
            icon: Icons.people_outlined,
            selectedIcon: Icons.people,
            cupertinoIcon: CupertinoIcons.person_2,
          ),
          (
            tab: AppTab.settings,
            label: localizations.tabSettings,
            icon: Icons.settings_outlined,
            selectedIcon: Icons.settings,
            cupertinoIcon: CupertinoIcons.gear,
          ),
        ];

    if (isApple) {
      return CupertinoTabBar(
        currentIndex: currentTab.index,
        items: [
          for (final item in items) ...[
            BottomNavigationBarItem(
              icon: Icon(
                item.cupertinoIcon,
                key: ValueKey('tab-${item.tab.name}'),
              ),
              label: item.label,
            ),
          ],
        ],
        onTap: (index) => onTabSelected(items[index].tab),
      );
    }

    return NavigationBar(
      selectedIndex: currentTab.index,
      destinations: [
        for (final item in items)
          NavigationDestination(
            icon: Icon(item.icon, key: ValueKey('tab-${item.tab.name}')),
            selectedIcon: Icon(
              item.selectedIcon,
              key: ValueKey('tab-${item.tab.name}-selected'),
            ),
            label: item.label,
          ),
      ],
      onDestinationSelected: (index) => onTabSelected(items[index].tab),
    );
  }

  Widget buildSwitchTile({
    required String title,
    required String subtitle,
    required bool value,
    required ValueChanged<bool> onChanged,
  }) {
    if (isApple) {
      return buildListRow(
        title: title,
        subtitle: subtitle,
        trailing: CupertinoSwitch(value: value, onChanged: onChanged),
      );
    }

    return SwitchListTile(
      title: Text(title),
      subtitle: Text(subtitle),
      value: value,
      onChanged: onChanged,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16),
    );
  }

  Widget buildTextField({
    required TextEditingController controller,
    required String label,
    bool enabled = true,
    bool obscureText = false,
    bool showPasswordVisibilityToggle = false,
    List<String>? autofillHints,
  }) {
    if (isApple) {
      if (showPasswordVisibilityToggle && obscureText) {
        return _CupertinoPasswordTextField(
          controller: controller,
          enabled: enabled,
          placeholder: label,
        );
      }
      return Builder(
        builder: (context) {
          return CupertinoTextField(
            controller: controller,
            enabled: enabled,
            obscureText: obscureText,
            placeholder: label,
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: CupertinoDynamicColor.resolve(
                CupertinoColors.secondarySystemGroupedBackground,
                context,
              ),
              borderRadius: BorderRadius.circular(12),
            ),
          );
        },
      );
    }

    if (showPasswordVisibilityToggle && obscureText) {
      return _MaterialPasswordTextField(
        controller: controller,
        enabled: enabled,
        label: label,
        autofillHints: autofillHints,
      );
    }

    return TextField(
      controller: controller,
      enabled: enabled,
      obscureText: obscureText,
      autofillHints: autofillHints,
      decoration: InputDecoration(
        labelText: label,
        border: OutlineInputBorder(),
      ),
    );
  }

  Widget buildIconAction({
    required IconData materialIcon,
    IconData? cupertinoIcon,
    required VoidCallback onPressed,
  }) {
    if (isApple) {
      return CupertinoButton(
        padding: EdgeInsets.zero,
        minimumSize: Size.zero,
        onPressed: onPressed,
        child: Icon(cupertinoIcon ?? CupertinoIcons.add),
      );
    }

    return IconButton(onPressed: onPressed, icon: Icon(materialIcon));
  }

  Widget buildPrimaryButton({
    required VoidCallback? onPressed,
    required String label,
  }) {
    if (isApple) {
      return CupertinoButton.filled(onPressed: onPressed, child: Text(label));
    }

    return FilledButton(onPressed: onPressed, child: Text(label));
  }

  Widget buildSection({required Widget child}) {
    if (isApple) {
      return Builder(
        builder: (context) {
          return Container(
            decoration: BoxDecoration(
              color: CupertinoDynamicColor.resolve(
                CupertinoColors.secondarySystemGroupedBackground,
                context,
              ),
              borderRadius: BorderRadius.circular(14),
            ),
            padding: const EdgeInsets.all(18),
            child: child,
          );
        },
      );
    }

    return Card(
      child: Padding(padding: const EdgeInsets.all(20), child: child),
    );
  }

  Widget buildListRow({
    required String title,
    String? subtitle,
    Widget? trailing,
    VoidCallback? onTap,
    bool showDivider = false,
  }) {
    if (isApple) {
      return Builder(
        builder: (context) {
          final textTheme = CupertinoTheme.of(context).textTheme;
          final dividerColor = CupertinoDynamicColor.resolve(
            CupertinoColors.separator,
            context,
          );

          final content = Padding(
            padding: const EdgeInsets.symmetric(
              vertical: 14,
              horizontal: 12,
            ),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        title,
                        style: textTheme.textStyle,
                        textAlign: TextAlign.start,
                      ),
                      if (subtitle != null) ...[
                        const SizedBox(height: 4),
                        Text(
                          subtitle,
                          style: textTheme.textStyle.copyWith(
                            color: CupertinoDynamicColor.resolve(
                              CupertinoColors.secondaryLabel,
                              context,
                            ),
                          ),
                          textAlign: TextAlign.start,
                        ),
                      ],
                    ],
                  ),
                ),
                if (trailing != null) ...[const SizedBox(width: 12), trailing],
              ],
            ),
          );

          final child = onTap == null
              ? content
              : CupertinoButton(
                  padding: EdgeInsets.zero,
                  onPressed: onTap,
                  child: content,
                );

          return DecoratedBox(
            decoration: BoxDecoration(
              color: CupertinoDynamicColor.resolve(
                CupertinoColors.secondarySystemGroupedBackground,
                context,
              ),
              border: showDivider
                  ? Border(top: BorderSide(color: dividerColor, width: 0.5))
                  : null,
            ),
            child: child,
          );
        },
      );
    }

    return ListTile(
      title: Text(title),
      subtitle: subtitle == null ? null : Text(subtitle),
      onTap: onTap,
      trailing: trailing,
    );
  }

  Widget buildSegmentedControl<T extends Object>({
    required T selectedValue,
    required List<({T value, String label})> options,
    required ValueChanged<T> onSelected,
  }) {
    if (isApple) {
      return CupertinoSlidingSegmentedControl<T>(
        groupValue: selectedValue,
        children: {
          for (final option in options)
            option.value: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 8),
              child: Text(option.label),
            ),
        },
        onValueChanged: (value) {
          if (value != null) {
            onSelected(value);
          }
        },
      );
    }

    return SegmentedButton<T>(
      segments: [
        for (final option in options)
          ButtonSegment(
            value: option.value,
            label: Text(option.label),
          ),
      ],
      selected: {selectedValue},
      onSelectionChanged: (selection) => onSelected(selection.first),
    );
  }
}

class _MaterialPasswordTextField extends StatefulWidget {
  const _MaterialPasswordTextField({
    required this.controller,
    required this.enabled,
    required this.label,
    required this.autofillHints,
  });

  final TextEditingController controller;
  final bool enabled;
  final String label;
  final List<String>? autofillHints;

  @override
  State<_MaterialPasswordTextField> createState() =>
      _MaterialPasswordTextFieldState();
}

class _MaterialPasswordTextFieldState
    extends State<_MaterialPasswordTextField> {
  bool _obscureText = true;

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: widget.controller,
      enabled: widget.enabled,
      obscureText: _obscureText,
      autofillHints: widget.autofillHints,
      decoration: InputDecoration(
        labelText: widget.label,
        border: const OutlineInputBorder(),
        suffixIcon: IconButton(
          icon: Icon(
            _obscureText ? Icons.visibility_off : Icons.visibility,
          ),
          onPressed: () => setState(() => _obscureText = !_obscureText),
        ),
      ),
    );
  }
}

class _CupertinoPasswordTextField extends StatefulWidget {
  const _CupertinoPasswordTextField({
    required this.controller,
    required this.enabled,
    required this.placeholder,
  });

  final TextEditingController controller;
  final bool enabled;
  final String placeholder;

  @override
  State<_CupertinoPasswordTextField> createState() =>
      _CupertinoPasswordTextFieldState();
}

class _CupertinoPasswordTextFieldState
    extends State<_CupertinoPasswordTextField> {
  bool _obscureText = true;

  @override
  Widget build(BuildContext context) {
    return Builder(
      builder: (context) {
        return Row(
          children: [
            Expanded(
              child: CupertinoTextField(
                controller: widget.controller,
                enabled: widget.enabled,
                obscureText: _obscureText,
                placeholder: widget.placeholder,
                padding: const EdgeInsets.all(12),
                decoration: const BoxDecoration(
                  color: CupertinoColors.secondarySystemGroupedBackground,
                  borderRadius: BorderRadius.only(
                    topLeft: Radius.circular(12),
                    bottomLeft: Radius.circular(12),
                  ),
                ),
              ),
            ),
            CupertinoButton(
              padding: const EdgeInsets.only(left: 4),
              onPressed: () => setState(() => _obscureText = !_obscureText),
              child: Icon(
                _obscureText ? CupertinoIcons.eye_slash : CupertinoIcons.eye,
              ),
            ),
          ],
        );
      },
    );
  }
}

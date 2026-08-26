# AttendanceProMax

## How It Works

1. Scan the Oracle QR code — the app validates the URL, then starts a **silent background check-in** for every active user at once and opens a live status page (`/attendance-status`) showing each user's progress: queued / processing / done / failed / QR expired.
2. **Concurrency model**: up to 3 headless WebView instances run in parallel; extra users queue FIFO. A finished instance is *reused* for the next queued user instead of being recreated, because QR codes expire quickly.
3. If the QR expires mid-run, remaining jobs abort globally and the page prompts a fresh scan.
4. Reliability details: cookies are cleared between job dispatches (instances share one browser cookie jar), stale bridge events are rejected via per-job run IDs, and each job has a watchdog timeout.

The engine lives in `lib/features/attendance_automation/`: `models/`, `helpers/attendance_page_classifier.dart`, `services/` (orchestrator + headless worker), `providers/automation_providers.dart`.

## Getting Started

### Development

- Flutter SDK
- Android Studio / VS Code with Flutter extensions
- A physical device or emulator (camera access is required for scanning)

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/kowx712/attendancepromax_flutter.git
   cd attendancepromax_flutter
   ```

2. **Install dependencies**:
   ```bash
   flutter pub get
   ```

3. **Run the app**:
   ```bash
   flutter run
   ```

### Building

#### Android

```bash
flutter build apk --target-platform android-arm64 -v
```
The generated APK will be located at: `build/app/outputs/flutter-apk/app-release.apk`

#### iOS

Building for iOS requires **macOS** with **Xcode** installed.

1. Initialize the iOS project:
   ```bash
   flutter build ios
   ```
2. Open the project in Xcode:
   ```bash
   open ios/Runner.xcworkspace
   ```
3. Build and archive using Xcode or run via CLI:
   ```bash
   flutter build ipa
   ```

## Credits

This project is a Flutter-based evolution of [labubuu123/attendancepromax](https://github.com/labubuu123/attendancepromax)

## License

This project is licensed under the [MIT License](LICENSE).

# Building Royal Calendar

[Project page](../README.md) · [Release guide](PUBLISHING.md)

## Requirements

- JDK 17.
- Android SDK Platform 36 and Build Tools 36.0.0.
- SDK path set in `ANDROID_HOME` or in an untracked `local.properties` file.

The included Gradle Wrapper uses Gradle 8.11.1 and verifies its SHA-256.
The Android Gradle Plugin is 8.6.1. The first build needs internet access.

## Build

Windows:

```powershell
.\gradlew.bat assembleDebug
```

Linux / macOS:

```sh
./gradlew assembleDebug
```

The development APK is `app/build/outputs/apk/debug/app-debug.apk`.
On Windows, `./build.ps1` also copies it to `output/RoyalCalendar.apk`.

`assembleRelease` produces an unsigned release APK. Sign it with a private
release key before distributing it; see [PUBLISHING.md](PUBLISHING.md).
Signing keys, SDKs, JDKs and build output are excluded from Git.

## Checks

```sh
./gradlew assembleDebug assembleRelease assembleDebugAndroidTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.viktor.royalcalendar.test/com.viktor.royalcalendar.WidgetChecks
```

The device runner reports `PASS:` or `FAIL:`. It checks calendar boundaries,
date rendering, transparency and widget registration without changing saved
application settings.

GitHub Actions builds the APKs and runs Android Lint. Connected-device checks
require a phone or emulator and run separately.

# AfterSleep

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](LICENSE) [![Platform: Android](https://img.shields.io/badge/Platform-Android-green.svg?style=for-the-badge)](#) [![Total Github Downloads (All Assets)](https://img.shields.io/github/downloads/nihaltp/AfterSleep/total?style=for-the-badge&logo=github)](https://github.com/nihaltp/AfterSleep/releases/latest)

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="100" height="100" align="right" alt="AfterSleep Icon">

AfterSleep is a Kotlin + Jetpack Compose Android app for delayed media playback control.

## Download

Available on my F-Droid repository:

<img src="https://raw.githubusercontent.com/nihaltp/fdroid/main/repo/index.png" width="100" height="100" align="right" alt="F-Droid QR Code">

**Repository URL:**

```text
https://nihaltp.github.io/fdroid/repo/
```

[Open Repo Page](https://nihaltp.github.io/fdroid/repo/) to scan the QR code.

## Highlights

- Dark-only Material 3 UI
- MediaSessionManager and MediaController based playback control
- Foreground service for reliable timers
- Notification listener support for active session detection
- MVVM architecture with StateFlow and coroutines
- Settings for default delay, stop-after, dim screen, monochrome mode, and fallback behavior

## Project layout

- `app/src/main/java/com/nihaltp/aftersleep/data` - repositories and app container
- `app/src/main/java/com/nihaltp/aftersleep/media` - media session discovery and playback control
- `app/src/main/java/com/nihaltp/aftersleep/service` - foreground service and notification listener service
- `app/src/main/java/com/nihaltp/aftersleep/ui` - Compose UI, screens, components, and theme
- `app/src/main/java/com/nihaltp/aftersleep/viewmodel` - viewmodels

## Notes

- Grant notification access and notification permission for the best experience.
- Battery optimization can interfere with overnight timers, so the app includes a shortcut to the relevant Android settings screen.

## Screenshots

Below are phone screenshots included in the `fastlane` metadata folder.

<p align="center">
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="240" alt="Screenshot 1" />
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="240" alt="Screenshot 2"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="240" alt="Screenshot 3"/>
  <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" width="240" alt="Screenshot 4"/>
</p>

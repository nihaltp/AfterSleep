# Changelog

## [1.0.3] - 2026-11-21

### Added

- 13c0f6e  feat: add github details card
- 3df282d  feat: add info card in settings
- df41e82  feat: add adaptive icon with monochrome support

### Fixed

- da2ecae  fix: universal APK versionCode generation

### Style

- b6ffaeb  style: format files with pre-commit

### Documentation

- f06a145  docs: update README with F-Droid repository link and improve screenshot section

### Maintenance

- 6ad1978  chore: enable apk size shrinking
- 429d654  chore: remove markdown in description
- d7b70f4  chore: add fastlane
- 13b3917  chore: add fastlane screenshot
- dea7b6f  chore: update screenshots
- 90dd31a  chore: add pre-commit
- 2ad6de7  chore: create dependabot.yml
- 19e5950  chore: add mergify to auto merge pr
- fe3b441  chore: bump androidx.datastore:datastore-preferences from 1.1.1 to 1.2.1
- 08e9834  chore: bump org.jetbrains.kotlinx:kotlinx-coroutines-test in /app
- 78bb614  chore: bump pre-commit to v6.0.0
- 090f595  chore: reduce jvm heap to 2048m
- 2d241e0  chore: downgraded kotlinx-coroutines to 1.8.1
- 1fcb708  chore: Suppress the AGP 8.5.2 / compileSdk 35 warning
- 5152df2  chore: use html in fastlane files
- 3bc3693  chore: bump org.jetbrains.kotlinx:kotlinx-coroutines-test in /app
- 377d389  chore: bump org.jetbrains.kotlin.android from 1.9.24 to 2.4.0
- ee49eef  chore: bump fastlane from 2.236.1 to 2.237.0
- dd256c4  chore: Use RAM percentage for Gradle JVM heap
- 7170c1c  chore: Refactor release prep into Fastfile helper
- 3647c85  chore: Add retries for GitHub release creation
- 2af421d  chore: add CI test
- 88730b0  chore: update Kotlin options to use new DSL for JVM target
- 146ea71  chore: update README to include additional badges
- f1de6b2  chore: Remove ABI split build configuration
- 6dfdc39  chore: add AboutLibraries plugin and implement Licenses screen
- 29d7814  chore: bump io.mockk:mockk from 1.13.11 to 1.14.11 in /app
- 0984389  chore: bump gradle-wrapper from 9.4.1 to 9.6.1
- 0201ec4  chore: update ktlint hooks to use Gradle wrapper directly
- eab0dad  chore: integrate Detekt for static code analysis and remove unused dependencies
- 95dfcf3  chore: remove build release APK step from CI workflow
- 005aef3  chore: add detekt step to CI workflow for static code analysis
- e39fd34  chore: update activeSessionRepository initialization to remove context dependency
- f650023  chore: enhance keystore validation in build.gradle.kts for release signing
- f41fada  chore: update CI workflow to allow workflow_call
- aafc22c  chore: add release workflow for building and publishing APKs
- c21cea8  chore: refactor resolve_abi_apk to simplify APK selection logic
- 1234b1c  chore: add releaseScreenshot variant for APK builds and update screenshot capture process
- 7438d92  chore: update AndroidManifest to include media-related intents
- 52ab749  chore: enhance release preparation process
- 51d9db2  chore: update Gemfile.lock
- e17f79e  chore: add Android SDK packages for API level 35 in CI workflows
- 689da7e  chore: update release workflow to specify APK file path for GitHub release
- 0ffc014  chore: enhance CI workflow by splitting jobs
- f3ea13b  chore: update keystore properties path in release workflow
- 5eaa79c  chore: add step to decode keystore in release workflow

### Other

- ee50124  Add app icon; update screenshot names
- 172363a  Add unit tests for UserSettings, MediaSessionSnapshot, PlaybackCoordinator, PermissionStateSnapshot, and SettingsViewModel
- 33e8880  rename changelog to changelogs
- 5e15bb3  Add ABI splits and version code mapping for multiple architectures

# Changelog

## v1.0.2 - 2026-05-23

- Add `TimerChip` "Off" option and refine selected delay display logic (a43b124)
- Refactor screen brightness logic for better readability (414966d)
- Refactor code for improved readability and formatting (0354e1e)

## v1.0.1 - 2026-05-16

- Add splash screen

## v1.0.0 - 2026-05-15

- Add MIT License to the project (5967b6e)
- Add signing configuration for release builds in build.gradle.kts (2fc80fe)
- Add app metadata including title, short and full descriptions, and screenshots (ccc651d)
- Update app icon in AndroidManifest and add ic_launcher image (4064932)
- Implement playback management and notification handling with new controllers (9f401d0)
- Refactor permission handling in HomeScreen to conditionally display PermissionsBlock (db6222e)
- Initial Commit (187c74d)

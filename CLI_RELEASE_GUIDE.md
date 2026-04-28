# Ubuntu CLI Build & Release Transition Plan

This document outlines the strategy for transitioning Varisankya's Android build and Play Store release process from a Windows/Android Studio environment to a headless Ubuntu environment using Gemini CLI.

## Goal
Enable fully automated creation of Debug APKs and Production App Bundles, including direct pushes to the Google Play Store, from this Linux machine.

## Phase 1: Environment Setup (COMPLETED)
- **JDK 17**: Installed via apt.
- **Android SDK Command-line Tools**: Installed at `~/Android/Sdk` (`cmdline-tools;latest`, `platforms;android-36`, `build-tools;36.0.0`).
- **Environment Variables**: `ANDROID_HOME` is set in `~/.bashrc`.
- **SDK Path**: `local.properties` contains `sdk.dir=/home/aarsh/Android/Sdk`.
- **Firebase Auth (Google Sign-In)**: The default `debug.keystore` generated on a new Linux environment has a unique SHA-1. This SHA-1 must be manually registered in the Firebase Console (or via `firebase apps:android:sha:create`) for Google Sign-In to function properly on debug builds.

## Phase 2: Secret Injection (COMPLETED)
- **Bitwarden CLI (`bw`)**: Installed and configured.
- **Automated Script**: `./retrieve_secrets.sh` securely unlocks the vault, downloads the `Varisankya` secure note, and injects:
  1. `app/google-services.json`
  2. `varisankya-upload-key`
  3. Release signing credentials into `local.properties` (non-committed).

## Phase 3: Automation Integration (COMPLETED)
To automate Play Store pushes, we use the **Gradle Play Publisher (GPP)** plugin.
- **Dependency**: Added `com.github.triplet.play` (v4.0.0) to `build.gradle.kts` files.
- **Auth**: A Service Account JSON key from the Google Play Console is securely retrieved from Bitwarden via `./retrieve_secrets.sh` and placed at `app/play_console_key.json` (ignored by Git).

## Phase 4: Execution
- **Debug APK**: `./gradlew assembleDebug`
- **Release APK**: `./gradlew assembleRelease`
- **Play Store Push**: By default, publishing targets the **Internal** testing track. To push to a specific track, pass the `playTrack` property:
  - Internal: `./gradlew publishBundle`
  - Closed (Alpha): `./gradlew publishBundle -PplayTrack=alpha`
  - Open (Beta): `./gradlew publishBundle -PplayTrack=beta`
  - Production: `./gradlew publishBundle -PplayTrack=production`

## Phase 5: GitHub Release Formatting
When publishing pre-releases or final releases to GitHub, always ensure the release notes are detailed and strictly formatted:
- **Title**: `vX.Y[-alpha.Z] - Concise Feature Summary [Emoji]` (e.g., `v3.8-alpha.2 - Login Crash Fix 🩹`)
- **Body**: Split into clear sections:
  ```markdown
  Brief summary of the release's purpose.

  ## ✨ What's New
  • Bullet point detailing a new feature or change.

  ## 🛠 Fixes & Improvements
  • Bullet point detailing a fix, like resolving a NullPointerException.
  • App Version Bump updated to [Version] (Version Code [Code]).

  Enjoying Varisankya? Consider leaving a star on GitHub! ⭐️
  ```

## Current Status
- Transitioned successfully to Ubuntu CLI on April 27, 2026.
- Pre-release APKs can be generated and published to GitHub.
- Google Play automation (GPP) is integrated and ready. Run `./retrieve_secrets.sh` to extract the key, and then `./gradlew publishBundle` to push to the store.

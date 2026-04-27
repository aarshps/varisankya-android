# Ubuntu CLI Build & Release Transition Plan

This document outlines the strategy for transitioning Varisankya's Android build and Play Store release process from a Windows/Android Studio environment to a headless Ubuntu environment using Gemini CLI.

## Goal
Enable fully automated creation of Debug APKs and Production App Bundles, including direct pushes to the Google Play Store, from this Linux machine.

## Phase 1: Environment Setup (COMPLETED)
- **JDK 17**: Installed via apt.
- **Android SDK Command-line Tools**: Installed at `~/Android/Sdk` (`cmdline-tools;latest`, `platforms;android-36`, `build-tools;36.0.0`).
- **Environment Variables**: `ANDROID_HOME` is set in `~/.bashrc`.
- **SDK Path**: `local.properties` contains `sdk.dir=/home/aarsh/Android/Sdk`.

## Phase 2: Secret Injection (COMPLETED)
- **Bitwarden CLI (`bw`)**: Installed and configured.
- **Automated Script**: `./retrieve_secrets.sh` securely unlocks the vault, downloads the `Varisankya` secure note, and injects:
  1. `app/google-services.json`
  2. `varisankya-upload-key`
  3. Release signing credentials into `local.properties` (non-committed).

## Phase 3: Automation Integration (PENDING)
To automate Play Store pushes, we will use the **Gradle Play Publisher (GPP)** plugin.
- **Dependency**: Add `com.github.triplet.play` to `build.gradle.kts`.
- **Auth**: A Service Account JSON key from the Google Play Console must be retrieved and stored securely on the machine (referenced by GPP).

## Phase 4: Execution
- **Debug APK**: `./gradlew assembleDebug`
- **Release APK**: `./gradlew assembleRelease`
- **Play Store Push (Future)**: `./gradlew publishReleaseBundle`

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
- Next milestone: Phase 3 (Google Play automation).

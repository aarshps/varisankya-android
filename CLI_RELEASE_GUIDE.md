# Ubuntu CLI Build & Release Transition Plan

This document outlines the strategy for transitioning Varisankya's Android build and Play Store release process from a Windows/Android Studio environment to a headless Ubuntu environment using Gemini CLI.

## Goal
Enable fully automated creation of Debug APKs and Production App Bundles, including direct pushes to the Google Play Store, from this Linux machine.

## Phase 1: Environment Setup
The environment requires the following dependencies (currently missing as of April 2026):
- **JDK 17**: Required for the current Gradle/AGP version.
- **Android SDK Command-line Tools**: Specifically `cmdline-tools;latest`, `platforms;android-36`, and `build-tools;36.0.0`.
- **Environment Variables**: `ANDROID_HOME` must be set and added to the path for future agents to use `sdkmanager` and `gradlew`.

## Phase 2: Secret Injection (Manual Setup)
To sign release builds, the following must be recovered from Bitwarden (see `PUBLISHING_SECRETS_RECOVERY.md`):
1. **Keystore**: `varisankya-upload-key` (decoded from Base64).
2. **Google Services**: `app/google-services.json` (decoded from Base64).
3. **Credentials**: `Key Alias`, `Keystore Password`, and `Key Password` must be placed in `gradle.properties` (non-committed) or environment variables.

## Phase 3: Automation Integration
To automate Play Store pushes, we will use the **Gradle Play Publisher (GPP)** plugin.
- **Dependency**: Add `com.github.triplet.play` to `build.gradle.kts`.
- **Auth**: A Service Account JSON key from the Google Play Console must be retrieved and stored securely on the machine (referenced by GPP).

## Phase 4: Execution
- **Debug APK**: `./gradlew assembleDebug`
- **Release Bundle**: `./gradlew bundleRelease`
- **Play Store Push**: `./gradlew publishReleaseBundle`

## Current Status
- Research completed on April 26, 2026.
- Feasibility confirmed.
- Waiting for Phase 1 execution (installation of JDK/SDK).

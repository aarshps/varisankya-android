---
name: Headless Linux Android Builds
description: Guidelines for compiling Varisankya from the CLI without Android Studio.
---

# Headless Linux Build Environment

Varisankya is now built headlessly using the Gemini CLI on an Ubuntu environment. Android Studio is **not** required.

## Key Infrastructure Details
- **JDK:** OpenJDK 17.
- **SDK Path:** The Android SDK is installed manually to `~/Android/Sdk`.
- **Environment:** `ANDROID_HOME` is set globally in `~/.bashrc`.
- **Local Overrides:** `local.properties` contains `sdk.dir=/home/aarsh/Android/Sdk`.

## Building the App
Run Gradle non-interactively using the CLI wrapper.
- **Debug Build:** `./gradlew assembleDebug` (Outputs to `app/build/outputs/apk/debug/app-debug.apk`)
- **Release Build:** `./gradlew assembleRelease` (Outputs to `app/build/outputs/apk/release/app-release.apk`)

## Missing SDK Errors
If the build fails complaining about "SDK location not found", ensure `local.properties` exists in the workspace root with the correct `sdk.dir` definition.

## Validation
Always build the app completely (`assembleDebug` or `assembleRelease`) to empirically validate code changes. Do not assume syntax is correct without compilation.
# Varisankya - Smart Subscription Manager

Varisankya is a modern, high-performance Android application designed to help users track and manage their recurring subscriptions with ease. Built with a focus on the **Material 3 Expressive (M3E)** design language, it offers a tactile, personalized, and visually stunning experience.

## ✨ Latest Release: v2.0

This major release introduces user-configurable reminders, improved splash screen visuals, and official privacy documentation for Play Store compliance.

### What's New in v2.0
*   **Configurable Notification Timing**: Users can now set their preferred time for daily subscription reminders directly from the Settings menu using a Material 3 Time Picker.
*   **Privacy-First Design**: Notifications have been set to `IMPORTANCE_LOW` to avoid cluttering the status bar while still providing timely reminders.
*   **Refined Splash Screen**: Fixed icon scaling and clipping issues on the splash screen to ensure the full app icon is displayed beautifully on Android 12+ devices.
*   **Official Privacy Policy**: Integrated a comprehensive Privacy Policy accessible within the app and hosted on GitHub to meet Google Play Store requirements.
*   **Improved Work Scheduling**: Enhanced notification worker logic to dynamically reschedule based on user-defined time preferences.

### Previous Highlights (v1.4)
*   **Themed Vector Branding**: Intelligent Black/White logo flipping based on system theme.
*   **Unified Status Pillar**: Reimagined due days indicator with integrated progress tracking.
*   **Precision UI Alignment**: Standardized list layouts and urgency indicators.
*   **Immersive Haptic Feedback**: Tactile responses integrated throughout the core user journey.

## 🚀 Key Features

*   **Customizable Reminders**: Schedule notifications at a time that suits your daily routine.
*   **Subscription Tracking**: Effortlessly manage costs, recurrence, and due dates.
*   **Usage-Based Personalization**: The app learns your preferences, bubbling up your most used choices.
*   **Secure Authentication**: Google Sign-In integration for seamless cloud sync.
*   **Cloud Architecture**: Real-time data persistence with Firebase Firestore.

## 🛠 Technical Specifications

*   **Platform**: Android 15+ (API 35+)
*   **Language**: 100% Kotlin
*   **UI Framework**: Material Components 1.11.0 (Material 3)
*   **Backend**: Firebase (Auth & Firestore)

## 📦 Build & Release

To build the debug APK manually, run:
```bash
./gradlew :app:assembleDebug
```
The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

### Tagging Version v2.0
To freeze this state in Git:
```bash
git add .
git commit -m "chore: release version 2.0"
git tag -a v2.0 -m "Release version 2.0"
git push origin v2.0
```

## ⚙️ Setup

1.  Place your `google-services.json` in the `app/` folder.
2.  Ensure Firestore Rules allow sub-collection access.
3.  Sync and Run from Android Studio.

## 📜 License
This project is licensed under the MIT License.

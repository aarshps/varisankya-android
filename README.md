# Varisankya - Smart Subscription Manager

Varisankya is a modern, high-performance Android application designed to help users track and manage their recurring subscriptions with ease. Built with a focus on the **Material 3 Expressive (M3E)** design language, it offers a tactile, personalized, and visually stunning experience.

## ✨ Latest Release: v2.6

This release introduces a refined visual identity for subscription items, focusing on clarity, legibility, and expressive motion.

### What's New in v2.6
*   **Expressive Status Pills**: Reimagined the status indicator as a "fattier" and rounder pill using Material 3 Expressive container colors (Tertiary, Secondary, Error) for immediate visual recognition.
*   **Decoupled Progress Bar**: Separated the progress bar from the status text, placing it below the pill for a cleaner, less cluttered look.
*   **Enhanced Legibility**: Optimized text colors to ensure perfect contrast against pastel container backgrounds, adhering to accessibility standards.
*   **Refined Layout**: Increased padding and spacing within list items (20dp/24dp) to create a more spacious, premium feel consistent with M3E guidelines.
*   **Smooth Animations**: Added smooth deceleration animations for progress bar fills when loading the list.

### Previous Highlights (v2.4)
*   **Typography Overhaul**: Adopted **Google Sans Flex** with maximum roundness across the entire application for a softer, more modern, and approachable aesthetic.
*   **Font Customization**: Added a new **Typography** setting, allowing users to switch between the custom rounded font and the system default font seamlessly.
*   **Dynamic Theming Engine**: Improved the base activity architecture to support instant theme switching across all screens without restarting the app.
*   **Global Consistency**: Unified typography in all surfaces, including Material components, headers, and body text, while preserving Dynamic Colors (Material You).

### Previous Highlights (v2.3)
*   **UI Stability**: Fixed visibility issues with the "Add Subscription" floating button to ensure seamless access to creation tools.
*   **Refined UX**: Improved transitions and view states when switching between logged-in and logged-out modes.
*   **Performance**: Optimized layout rendering for smoother scrolling experiences.

### Previous Highlights (v2.2)
*   **Search Functionality**: Implemented a full-screen search interface to easily find subscriptions.
*   **UI Polish**: Various UI enhancements and bug fixes.

### Previous Highlights (v2.0)
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
*   **Material 3 Expressive Design**: Utilizing the latest Material Design components and dynamic colors.
*   **Smart Interactions**: Haptic feedback and responsive animations.

## 🛠 Technical Specifications

*   **Platform**: Android 15+ (API 35+)
*   **Language**: 100% Kotlin
*   **UI Framework**: Material Components 1.14.0-alpha08 (Material 3)
*   **Backend**: Firebase (Auth & Firestore)

## 📦 Build & Release

To build the debug APK manually, run:
```bash
./gradlew :app:assembleDebug
```
The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

### Tagging Version v2.6
To freeze this state in Git:
```bash
git add .
git commit -m "chore: release version 2.6"
git tag -a v2.6 -m "Release version 2.6"
git push origin v2.6
```

## ⚙️ Setup

1.  Place your `google-services.json` in the `app/` folder.
2.  Ensure Firestore Rules allow sub-collection access.
3.  Sync and Run from Android Studio.

## 📜 License
This project is licensed under the MIT License.
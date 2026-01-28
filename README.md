# Varisankya - Smart Subscription Manager

Varisankya is a modern, high-performance Android application designed to help users track and manage their recurring subscriptions with ease. Built with a focus on the **Material 3 Expressive (M3E)** design language, it offers a tactile, personalized, and visually stunning experience.

## 🚀 Key Features

### 💎 Material 3 Expressive Design
*   **Expressive UI**: Utilizing the latest Material Design 3 components, dynamic colors (Material You), and "Expressive" container colors for a vibrant look.
*   **Refined Typography**: Features **Google Sans Flex** with maximum roundness for a soft, modern aesthetic. Users can toggle between this custom font and the system default via Settings.
*   **Smart Status Indicators**: Subscriptions feature "floating" status pills with subtle shadows and dynamic 1dp colored borders (Red for overdue, Cyan for upcoming, Gray for standard) to signal urgency at a glance.
*   **Visual Progress Tracking**: A decoupled, always-visible progress meter provides immediate context on payment timelines.
*   **Fluid Motion**: Smooth deceleration animations and refined layout spacing ensure a premium, responsive feel.
*   **Rounded UI Mandate**: A strict adherence to rounded corners (24dp-100dp) across all elements—cards, buttons, and even icons (rounded caps/joins)—creates a friendly, organic feel.
*   **Standardized M3 Motion**: Animations are tuned to industry-standard values (Short: 100ms, Medium: 300ms, Long: 500ms) with `FastOutSlowIn` interpolators for natural, physical movement.
*   **Immersive Haptics & Silent UX**: A completely toast-free experience. All actions are verified via distinct haptic feedback patterns (Tick, Click, Heavy Click) and subtle UI transitions, ensuring a distraction-free environment.
*   **Themed Branding**: Intelligent logo adaptation that responds to system light/dark mode changes.
*   **Ultra Smooth Home Launch**: Optimized app start sequence using shimmer skeletons and M3E reveal animations for a zero-flash, premium landing.

### 📊 Smart Analytics & History
*   **Premium Charting**: Totally redesigned Payment History featuring a horizontally scrollable **Column Chart**.
    *   **Visuals**: Thick, rounded bars with "Chip" styled labels for effortless readability.
    *   **Interactive**: Auto-scrolls to the latest data and scales dynamically.
*   **Comprehensive Tracking**: Effortlessly manage costs, currencies, recurrence cycles, and due dates.
*   **Compact Smart Data**: Amounts are intelligently formatted (e.g., "$ 1.2k") for quick scanning, with bold typography for emphasis.
*   **Intelligent Search**: Full-screen search interface to quickly locate specific subscriptions.
*   **Usage-Based Personalization**: The app learns from your inputs, automatically prioritizing your frequently used categories and currencies.

### 🔔 Notification & Settings
*   **Unified Settings**: A streamlined Settings experience with "Chart" vs "List" view preferences integrated directly into the Appearance controls.
*   **Interactive Bottom Sheets**: Tactile drag handles that respond with scale animations and haptic feedback (`CLOCK_TICK`, `CONFIRM`) for a living, breathing UI.
*   **Customizable Reminders**: Schedule daily notifications at a time that suits your routine using a native Material 3 Time Picker.
*   **Non-Intrusive Alerts**: Notifications are designed with `IMPORTANCE_LOW` priority to provide timely reminders without cluttering your status bar.
*   **Dynamic Theming Engine**: Switch themes instantly across the entire app without needing a restart.

### 🔒 Security & Cloud
*   **Seamless Auth**: Secure Google Sign-In integration.
*   **Real-Time Sync**: Data is persisted and synced in real-time using Firebase Firestore, ensuring your data is available across devices.
*   **Privacy Focused**: Includes accessible Privacy Policy and Data Deletion controls.

## 🛠 Technical Specifications

*   **Platform**: Android 15+ (Min API 35, Target API 36)
*   **Language**: 100% Kotlin
*   **UI Framework**: Material Components 1.14.0-alpha08 (Material 3)
*   **Architecture**: MVVM with Clean Loading Policy
*   **Backend**: Firebase (Auth & Firestore)

## 📦 Build & Release

To build the debug APK manually, run:
```bash
./gradlew :app:assembleDebug
```
The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

## ⚙️ Setup

1.  Place your `google-services.json` in the `app/` folder.
2.  Ensure Firestore Rules allow sub-collection access.
3.  Sync and Run from Android Studio.

## 📜 License
This project is licensed under the MIT License.
---
name: Varisankya Project Architecture
description: Overview of the Android subscription tracking app architecture
---

# Varisankya Project Architecture

This skill provides an overview of the Varisankya Android app architecture.

## App Purpose

Subscription expense tracking application that helps users monitor recurring payments.

## Key Components

### Activities

| Activity | Purpose |
|----------|---------|
| `MainActivity` | Home screen with subscription list |
| `SearchActivity` | Search and filter subscriptions |
| `SettingsActivity` | App preferences |
| `UnifiedHistoryActivity` | Payment history across all subscriptions |

### Bottom Sheets

| Component | Purpose |
|-----------|---------|
| `AddSubscriptionBottomSheet` | Add/Edit subscription form |
| `SelectionBottomSheet` | Generic chip-based picker (Category, Recurrence) |
| `PaymentHistoryBottomSheet` | Per-subscription payment history |

### Adapters
 
| Adapter | Purpose |
|---------|---------|
| `SubscriptionAdapter` | Main list with Primary/Active coloring |
| `PaymentAdapter` | Payment history items |
| `PaymentHistoryAdapter` | Unified history timeline |
 
### Hero Insights (Cashflow)
 
The `MainActivity` Hero Card implements a **"Remaining Monthly Liability"** logic, calculating the sum of all future bills in the current month to provide a cashflow forecast.

### Utilities

| Utility | Purpose |
|---------|---------|
| `ThemeHelper` | M3 Dynamic Color resolution |
| `ChipHelper` | Centralized High-Contrast Chip styling |
| `PreferenceHelper` | SharedPreferences access |

| `Constants` | App-wide constants (categories, recurrences) |

### Custom Views

| View | Purpose |
|------|---------|
| `RoundedProgressView` | Circular progress in subscription pills |
| `PaymentHistoryChart` | Custom drawn payment timeline chart |


## Data Layer

- **Firebase Firestore** - Cloud database
- **Firebase Auth** - User authentication
- **Firestore Structure:**
  ```
  users/{userId}/
    subscriptions/{subscriptionId}
      payments/{paymentId}
  ```

## Theme System

- **M3 Dynamic Colors** - Wallpaper-based theming
- **Theme file:** `res/values/themes.xml`
- **Color resolution:** via `ThemeHelper.kt`

## Widget

- **WidgetProvider** - Home screen widget
- **WidgetUpdateHelper** - Widget data refresh
- Update frequency: 1 hour

## Key Preferences

- `notification_days` - Days before due date to notify
- `theme_mode` - System/Light/Dark
- `haptics_enabled` - Haptic feedback toggle
- `font_family` - Google Sans/System

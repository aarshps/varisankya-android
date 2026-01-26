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
 
The `MainActivity` Hero Card implements a **"Remaining Monthly Liability"** logic, calculating the sum of **all Overdue bills + future bills in the current month** to provide a strict cashflow forecast.

### Utilities

**util/**: Functional and visual core:
  - `AnimationHelper.kt`: **M3E Ultra-Expressive** central. Staggered entrances, tactile springs, and logo orchestrations.
  - `PreferenceHelper.kt`: Centralized **Haptic Engine**. Directs all TICK, CONFIRM, and segment feedback.
  - `ThemeHelper.kt`: M3 Dynamic Color resolution Bridge.
  - `ChipHelper.kt`: High-contrast chip orchestration.
  - `Constants.kt`: App-wide constants (categories, recurrences).

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
 
- **Brand Monochrome Identity** - Strict Black/Gray/White palette for a premium, unified experience.
- **M3E Expressive Standards** - 28dp card corners, multi-layered surfaceContainers, and high-contrast highlights.
- **Theme file:** `res/values/themes.xml`
- **Color resolution:** via `ThemeHelper.kt`, with manual resource mapping for widgets.
 
## Widget
 
- **UpcomingWidgetProvider** - Redesigned for M3E (detached cards, Hero area).
- **Hero Priority**: The most upcoming bill is featured in a prominent card (`bg_widget_item_hero`) with unique styling.
- **WidgetUpdateHelper** - Widget data refresh
- Update frequency: 1 hour
 
## Key Preferences
 
- `notification_days` - Days before due date to notify
- `theme_mode` - System/Light/Dark
- `haptics_enabled` - Haptic feedback toggle (Mechanical Tick enabled)
- `font_family` - Google Sans Flex / System

---
name: Subscription Pill 4-Tier Color Hierarchy
description: The 4-tier M3 color system for subscription status pills
---

# Subscription Pill 4-Tier Color Hierarchy

This skill documents the color hierarchy used for subscription status pills in the app.

## Color Tiers
 
 | State | Background | Text | Usage |
 |-------|-----------|------|-------|
 | **Active** | `colorPrimary` | `colorOnPrimary` | Any active subscription (Due Today/Tomorrow/Future) |
 | **Inactive** | `colorSurfaceVariant` | `colorOnSurfaceVariant` | Paused/Discontinued subscriptions |
 | **Amount Pill** | `colorSurfaceContainerHighest` | `colorOnSurface` | Secondary formatting for cost |
 
 ## Implementation Location
 
 ```
 app/src/main/java/com/hora/varisankya/SubscriptionAdapter.kt
 ```
 
 ## Logic
 
 We moved away from complex urgency gradients to a clean binary state:
 
 ```kotlin
 if (!subscription.active) {
     // INACTIVE
     holder.pillContainer.setCardBackgroundColor(surfaceVariant)
     holder.daysLeftTextView.setTextColor(onSurfaceVariant)
 } else {
     // ACTIVE (Uniform Priority)
     holder.pillContainer.setCardBackgroundColor(primary)
     holder.daysLeftTextView.setTextColor(onPrimary)
 }
 ```

## Progress Bar Colors

- **Track color:** Always `colorOutlineVariant`
- **Progress color:** Matches the tier's background color

## User Preference Integration

The notification window is user-configurable:
```kotlin
val notificationWindow = PreferenceHelper.getNotificationDays(context)
```

This determines the boundary between Tier 2 and Tier 3.

## Related Files

- `ThemeHelper.kt` - Color resolution
- `item_subscription.xml` - Pill layout
- `RoundedProgressView.kt` - Progress bar component

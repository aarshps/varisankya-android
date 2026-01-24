---
name: Subscription Pill 4-Tier Color Hierarchy
description: The 4-tier M3 color system for subscription status pills
---

# Subscription Pill 4-Tier Color Hierarchy

This skill documents the color hierarchy used for subscription status pills in the app.

## Color Tiers

| Tier | State | Background | Text | Usage |
|------|-------|-----------|------|-------|
| **1** | Overdue | `colorPrimary` | `colorOnPrimary` | Maximum urgency - past due date |
| **2** | Near Due | `colorTertiary` | `colorOnTertiary` | High urgency - within notification window |
| **3** | Future | `colorSecondaryContainer` | `colorOnSecondaryContainer` | Normal - due but outside window |
| **4** | Inactive | `colorSurfaceVariant` | `colorOnSurfaceVariant` | Disabled subscriptions |

## Implementation Location

```
app/src/main/java/com/hora/varisankya/SubscriptionAdapter.kt
```

## Tier Logic

```kotlin
when {
    daysLeft <= 0 -> {
        // TIER 1: OVERDUE - Primary (Boldest M3 color)
        holder.pillContainer.setCardBackgroundColor(primary)
        holder.daysLeftTextView.setTextColor(onPrimary)
    }
    daysLeft <= notificationWindow -> {
        // TIER 2: NEAR DUE - Tertiary (Distinct M3 accent)
        holder.pillContainer.setCardBackgroundColor(tertiary)
        holder.daysLeftTextView.setTextColor(onTertiary)
    }
    else -> {
        // TIER 3: FUTURE - SecondaryContainer (Soft M3 tonal)
        holder.pillContainer.setCardBackgroundColor(secondaryContainer)
        holder.daysLeftTextView.setTextColor(onSecondaryContainer)
    }
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

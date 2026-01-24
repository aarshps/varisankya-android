---
name: M3 Dynamic Color Resolution
description: How to properly resolve Material 3 Dynamic Colors in this Android/Kotlin project
---

# M3 Dynamic Color Resolution

This skill explains how to correctly resolve Material 3 (M3) Dynamic Color theme attributes in this Android project.

## Problem

Direct references to `com.google.android.material.R.attr.colorPrimary` and similar M3 attributes cause Kotlin compilation errors ("Unresolved reference"). This is a known issue with Kotlin's R-class handling.

## Solution: ThemeHelper

Use the `ThemeHelper` utility class located at:
```
app/src/main/java/com/hora/varisankya/util/ThemeHelper.kt
```

### Available Color Methods

| Method | M3 Attribute | Usage |
|--------|-------------|-------|
| `getPrimaryColor(context)` | colorPrimary | Maximum contrast (Tier 1) |
| `getOnPrimaryColor(context)` | colorOnPrimary | Text on Primary |
| `getTertiaryColor(context)` | colorTertiary | High contrast (Tier 2) |
| `getOnTertiaryColor(context)` | colorOnTertiary | Text on Tertiary |
| `getSecondaryContainerColor(context)` | colorSecondaryContainer | Medium contrast (Tier 3) |
| `getOnSecondaryContainerColor(context)` | colorOnSecondaryContainer | Text on SecondaryContainer |
| `getSurfaceVariantColor(context)` | colorSurfaceVariant | Low contrast (Tier 4/Inactive) |
| `getOnSurfaceVariantColor(context)` | colorOnSurfaceVariant | Text on SurfaceVariant |
| `getOnSurfaceColor(context)` | colorOnSurface | Standard text |
| `getOutlineVariantColor(context)` | colorOutlineVariant | Progress bar tracks |

### How It Works

ThemeHelper uses `Resources.getIdentifier()` with `context.packageName` to find merged theme attributes:

```kotlin
// CORRECT: Uses app's package name to find merged M3 attributes
val attrId = context.resources.getIdentifier(attrName, "attr", context.packageName)

// WRONG: Material package doesn't contain these at runtime
val attrId = context.resources.getIdentifier(attrName, "attr", "com.google.android.material")

// WRONG: Causes Kotlin compilation errors
MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimary, fallback)
```

### Example Usage

```kotlin
import com.hora.varisankya.util.ThemeHelper

// In any Activity, Fragment, or Adapter
val primaryColor = ThemeHelper.getPrimaryColor(context)
val tertiaryColor = ThemeHelper.getTertiaryColor(context)

// Apply to views
holder.pillContainer.setCardBackgroundColor(primaryColor)
chip.chipBackgroundColor = ColorStateList.valueOf(tertiaryColor)
```

## XML Resolution

For XML resources, use `?attr/` syntax which resolves correctly:

```xml
<item android:color="?attr/colorTertiary" android:state_checked="true"/>
<item android:color="?attr/colorSurfaceVariant"/>
```

These are defined in:
- `res/color/selector_chip_background_high_contrast.xml`
- `res/color/selector_chip_text_high_contrast.xml`

## Troubleshooting

### Error: "Unresolved reference 'colorPrimary'"

**Symptom:**
```
e: Unresolved reference 'colorPrimary'.
```

**Cause:** Kotlin cannot resolve `com.google.android.material.R.attr.colorPrimary` at compile time.

**Solution:** Use `ThemeHelper` instead of direct R.attr references.

### Error: All colors showing as gray

**Symptom:** All UI elements appear gray instead of theme colors.

**Cause:** Using wrong package name in `getIdentifier()`:
```kotlin
// WRONG - returns 0, triggering fallback color
getIdentifier(name, "attr", "com.google.android.material")
```

**Solution:** Use app's package name:
```kotlin
// CORRECT
getIdentifier(name, "attr", context.packageName)
```

### ContextThemeWrapper doesn't apply colors

**Symptom:** Chips created with `ContextThemeWrapper(..., R.style.Widget_App_Chip)` don't show selected colors.

**Cause:** XML selectors with `?attr/` don't resolve through ContextThemeWrapper properly.

**Solution:** Apply colors programmatically using ThemeHelper (see chip-styling skill).

## Related Skills

- **chip-styling** - High-contrast chip implementation
- **subscription-color-hierarchy** - 4-tier pill color system

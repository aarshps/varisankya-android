---
name: M3 Dynamic Color Resolution
description: How to properly resolve Material 3 Dynamic Colors in this Android/Kotlin project
---

# M3 Dynamic Color Resolution
 
 > [!IMPORTANT]
 > **Hybrid Design System**:
 > - **App**: Uses strict **MONOCHROME** (Black/Gray/White) identity.
 > - **Widget**: Uses **Wallpaper Orchestration** (Dynamic Colors) to natively integrate with the user's Home Screen (API 31+).
 
 This skill explains how to resolve these two distinct systems.
 
 ## Theme Resolution logic
 
 The app uses a strict mapping in `colors.xml` and `ThemeHelper.kt` to ensure color consistency across Activities, Bottom Sheets, and Widgets.
 
 ### Widget "Wallpaper Orchestration" (API 31+)

Widgets MUST use the `dynamic_*` color aliases located in `res/values/colors.xml` and `res/values-v31/colors.xml`. These aliases resolve to wallpaper-based system tokens on Android 12+.

| Logical Role | Resource Name | Purpose |
|--------------|---------------|---------|
| Root Surface | `@color/dynamic_widget_background` | Widget container base |
| Hero Surface | `@color/dynamic_hero_background` | Elevated "Next Payment" block |
| Pill Accent | `@color/dynamic_pill_background` | High-contrast status pill |
| Text Primary | `@color/dynamic_text_primary` | Names and titles |
| Text Accent | `@color/dynamic_text_accent` | Sub-labels and dates |

**Night Mode Parity**: Ensure `values-night` and `values-night-v31` provide sufficient contrast (e.g. Hero Light -> Pill Dark).
 
 ### Surface Hierarchy (Tonal Layering)
 
 To achieve the "Breezy" M3E look, use the correct container role for the background:
 
 | Role | Attribute | Usage |
 |------|-----------|-------|
 | **Base** | `colorSurfaceContainerLow` | Main screen window background. |
 | **Card** | `colorSurfaceContainer` | Subscription items (Standard). |
 | **Pill** | `colorSurfaceContainerHigh` | Highlight bubbles inside cards. |
 | **Hero** | `colorPrimary` | Highest impact highlights (Active/Urgent). |
 
 ## Problem: BaseActivity Theme Override
 
 A common issue in this project is that `BaseActivity` calls `setTheme()` to handle custom fonts (System vs Google Sans). This manual `setTheme()` call **wipes out** the Dynamic Color overlay applied by the Application class.
 
 **Solution:**
 You MUST re-apply dynamic colors *after* calling `setTheme()`:
 
 ```kotlin
 override fun onCreate(savedInstanceState: Bundle?) {
     // ... logic to check font pref ...
     setTheme(themeId) // <--- This wipes Dynamic Colors
     
     // VITAL: Re-apply overlay immediately
     com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
     
     super.onCreate(savedInstanceState)
 }
 ```
 
 ## Problem: Kotlin R-Class Resolution
 
 Direct references to `com.google.android.material.R.attr.colorPrimary` often cause "Unresolved reference" errors.

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
| `getSurfaceContainerHighColor(context)` | colorSurfaceContainerHigh | Distinct surface for chips/cards |
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
- `res/color/chip_background_color.xml`
- `res/color/chip_stroke_color.xml`
- `res/color/chip_text_color.xml`


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

### Surface Hierarchy (Tonal Layering)

To achieve the "Breezy" M3E look, use the correct container role for the background:

| Role | Attribute | Usage |
|------|-----------|-------|
| **Base** | `colorSurfaceContainerLow` | Main screen window background. |
| **Card** | `colorSurfaceContainer` | Subscription items, standard blocks. |
| **Pill** | `colorSurfaceContainerHigh` | Highlight bubbles, amount chips inside cards. |
| **Active** | `colorPrimary` | Highest urgency/active status highlights. |

## Related Skills


- **chip-styling** - High-contrast chip implementation
- **chart-visualization** - Standardized chart drawing and colors
- **header-actions** - Toolbar icon standards and order
- **subscription-color-hierarchy** - 4-tier pill color system


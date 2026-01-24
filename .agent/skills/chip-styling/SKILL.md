---
name: High-Contrast Chip Styling
description: How to apply consistent M3 high-contrast styling to chips throughout the app
---

# High-Contrast Chip Styling

This skill documents how chips are styled with M3 Dynamic Colors for high contrast.

## Design Spec

| State | Background | Text | Icon Tint |
|-------|-----------|------|-----------|
| **Selected** | `colorTertiary` | `colorOnTertiary` | `colorOnTertiary` |
| **Unselected** | `colorSurfaceVariant` | `colorOnSurface` | `colorOnSurface` |

## Shape Behavior

- **Selected:** Less rounded (12dp radius)
- **Unselected:** Fully rounded pill (100dp radius)

## Implementation Locations

### 1. XML-Defined Chips (Settings)

Location: `res/layout/activity_settings.xml`

Use the app's chip style:
```xml
<com.google.android.material.chip.Chip
    style="@style/Widget.App.Chip"
    ... />
```

### 2. Programmatically Created Chips

For `SearchActivity.kt` and `SelectionBottomSheet.kt`, apply colors in the update function:

```kotlin
private fun updateChipShape(chip: Chip) {
    val r = resources.displayMetrics.density
    val selectedRadius = 12f * r
    val unselectedRadius = 100f * r
    
    // Update shape
    chip.shapeAppearanceModel = chip.shapeAppearanceModel.toBuilder()
        .setAllCornerSizes(if (chip.isChecked) selectedRadius else unselectedRadius)
        .build()
    
    // Apply M3 Dynamic Colors using ThemeHelper
    if (chip.isChecked) {
        chip.chipBackgroundColor = ColorStateList.valueOf(
            ThemeHelper.getTertiaryColor(context)
        )
        chip.setTextColor(ThemeHelper.getOnTertiaryColor(context))
        chip.chipIconTint = ColorStateList.valueOf(
            ThemeHelper.getOnTertiaryColor(context)
        )
    } else {
        chip.chipBackgroundColor = ColorStateList.valueOf(
            ThemeHelper.getSurfaceVariantColor(context)
        )
        chip.setTextColor(ThemeHelper.getOnSurfaceColor(context))
        chip.chipIconTint = ColorStateList.valueOf(
            ThemeHelper.getOnSurfaceColor(context)
        )
    }
}
```

### 3. Chip Style Definition

Location: `res/values/themes.xml`

```xml
<style name="Widget.App.Chip" parent="Widget.Material3.Chip.Filter">
    <item name="chipStrokeColor">@color/chip_stroke_app</item>
    <item name="chipStrokeWidth">1dp</item>
    <item name="chipBackgroundColor">@color/selector_chip_background_high_contrast</item>
    <item name="android:textColor">@color/selector_chip_text_high_contrast</item>
    <item name="chipIconTint">@color/selector_chip_text_high_contrast</item>
</style>
```

## Color Selectors

### Background (`res/color/selector_chip_background_high_contrast.xml`)
```xml
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="?attr/colorTertiary" android:state_checked="true"/>
    <item android:color="?attr/colorSurfaceVariant"/>
</selector>
```

### Text (`res/color/selector_chip_text_high_contrast.xml`)
```xml
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="?attr/colorOnTertiary" android:state_checked="true"/>
    <item android:color="?attr/colorOnSurface"/>
</selector>
```

## Important: ContextThemeWrapper Limitation

Using `ContextThemeWrapper` to apply `Widget.App.Chip` does NOT work for color resolution because XML selectors with `?attr/` don't resolve properly through the wrapper. **Always apply colors programmatically** using `ThemeHelper` for dynamically created chips.

## Chip Locations in App

1. **Settings** - Theme, Font, Haptics chips (XML)
2. **Search** - Category filter chips (Programmatic)
3. **SelectionBottomSheet** - Recurrence, Category pickers (Programmatic)

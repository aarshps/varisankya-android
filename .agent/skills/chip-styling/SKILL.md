---
name: High-Contrast Chip Styling
description: How to apply consistent M3 high-contrast styling to chips throughout the app
---

# High-Contrast Chip Styling (M3E Expressive)

This skill documents the M3E Expressive standard for chips in Varisankya.

## Design Spec

| State | Background | Text | Shape | Stroke |
|-------|-----------|------|-------|--------|
| **Selected** | `colorPrimary` | `colorOnPrimary` | **20dp** Rounded Rect | `0dp` |
| **Unselected** | `colorSurfaceContainerHigh` | `colorOnSurface` | **Pill** (100dp) | `0.8dp` (`colorTertiary`) |

## Reasoning: Expressive Shapes

In M3E, selected states are more pronounced. We move away from the standard 8-12dp radius to a **20dp** corner for selected chips. This creates a more distinct "morphed" feeling when toggling.

## Implementation: ChipHelper.styleChip(chip)

Standardized logic for programmatic chips:

```kotlin
fun styleChip(chip: Chip) {
    val isChecked = chip.isChecked
    val r = context.resources.displayMetrics.density
    
    // Expressive Shape Transition
    val selectedRadius = 20f * r // M3E Large
    val unselectedRadius = 100f * r // Standard Pill
    
    chip.shapeAppearanceModel = chip.shapeAppearanceModel.toBuilder()
        .setAllCornerSizes(if (isChecked) selectedRadius else unselectedRadius)
        .build()
        
    // Tonal Color Policy
    if (isChecked) {
        // Active/Selected state now matches Home Screen Pills (Primary)
        chip.chipBackgroundColor = ColorStateList.valueOf(ThemeHelper.getPrimaryColor(context))
        chip.setTextColor(ThemeHelper.getOnPrimaryColor(context))
    } else {
        chip.chipBackgroundColor = ColorStateList.valueOf(ThemeHelper.getSurfaceContainerHighColor(context))
        chip.setTextColor(ThemeHelper.getOnSurfaceColor(context))
    }
}
```

## Rule of Thumb

Never use sharp corners (< 12dp) for interactive chips. They must feel fluid and touchable.

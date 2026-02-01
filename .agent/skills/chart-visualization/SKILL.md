---
name: Chart Visualization Standards
description: Guidelines for styling and drawing consistent charts (bars, labels, typography)
---

# Chart Visualization Standards (M3E Expressive)

This skill documents the visual standards for custom charts, specifically the `PaymentHistoryChart`.

## Color System (M3E Tonal)

Charts follow a tonal layering strategy to avoid "floating" boxiness:

| Element | Color Attribute | Usage |
|---------|----------------|-------|
| **Data Bars** | `?attr/colorPrimary` | Bold representative data. |
| **Label Chips** | `?attr/colorSurfaceContainerHigh` | Tonal highlights for Y-axis and value bubbles. |
| **Label Text** | `?attr/colorOnSurface` | Standard legible text. |
| **Grid Lines** | `?attr/colorOutlineVariant` | Subtle structural guides. |

## Typography (Expressive Scaling)

Labels must be large and clean, avoiding medium weights for a "breezy" look:

```kotlin
private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = 32f // Scaled up for M3E expressive readability
    textAlign = Paint.Align.CENTER
    typeface = Typeface.DEFAULT
}
```

## Drawing Standards
 
- **Corner Radius:** All chart-related shapes (bars, bubbles) must use at least **12dp** (or `24dp` for large containers) to match the M3E aesthetic.
- **Spacing:** Minimum **16dp** padding between chart elements and labels.
- **Antialiasing:** Always enabled for smooth, premium curves.
 
## Interaction Standards
 
- **Labels**: Horizontal Axis (Activity) and Legend (Bottom).
- **Chronological Grouping**: Months MUST be grouped and sorted using **zero-padded strings** (e.g., `String.format(Locale.US, "%d-%02d", year, month)`) to avoid lexical sorting errors where October (`9`) follows December (`11`).
- **Currency Parity**: All amount labels drawn in charts MUST follow the app-wide **50% smaller symbol** and **standard spacing** rules. This requires manual `Paint` size adjustments in `onDraw`.
- **Interactive Haptics**: Every bar tap MUST trigger `PreferenceHelper.performClickHaptic()`. A confirmed navigation (Drill Down) MUST trigger `performSuccessHaptic()`.
 
## Implementation ExamplentHistoryChart.kt
 
Always resolve colors via `ThemeHelper` in `onDraw` to ensure the chart respects the app's monochrome tonal policy.

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

## Implementation: PaymentHistoryChart.kt

Always resolve colors via `ThemeHelper` in `onDraw` to ensure the chart respects the app's monochrome tonal policy.

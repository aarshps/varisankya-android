---
name: Skeleton UI Standards
description: Guidelines for styling and transitioning from shimmer-ready skeleton placeholders
---

# Skeleton UI Standards

To maintain an "Ultra Smooth" loading experience, all data-driven screens must implement a skeleton UI that mimics the final content structure.

## Core Principles

1. **Structural Parity**: Skeletons must match the exact proportions, margins, and shapes of the real UI components.
2. **Surface Tonality**: Use `?attr/colorSurfaceContainerHigh` or `?attr/colorSurfaceContainerHighest` for the skeleton blocks to distinguish them while maintaining a flat M3 aesthetic.
3. **Implicit Shimmer**: Skeletons do not need aggressive animations; the movement comes from the transition into real data.

## Implementation Pattern

### 1. The Skeleton Layout
Create a composite layout (e.g., `layout_home_skeleton.xml`) that includes multiple item skeletons:
```xml
<LinearLayout ...>
    <include layout="@layout/item_skeleton_hero" />
    <include layout="@layout/item_skeleton_subscription" />
</LinearLayout>
```

### 2. The Transition
Use `AnimationHelper.animateReveal` and a subtle haptic to bridge the gap between "Loading" and "Ready":
```kotlin
if (loadingSkeleton.visibility == View.VISIBLE) {
    loadingSkeleton.visibility = View.GONE
    mainContentWrapper.visibility = View.VISIBLE
    AnimationHelper.animateReveal(mainContentWrapper)
    PreferenceHelper.performClickHaptic(mainContentWrapper)
}
```

## Reveal Parameters
- **Duration**: 400ms - 500ms
- **Interpolator**: `EMPHASIZED` (M3 Standard)
- **Haptic**: `performClickHaptic` or `SEGMENT_TICK`

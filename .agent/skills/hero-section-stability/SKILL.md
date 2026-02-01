---
name: Hero Section UI Stability
description: Best practices for stable, flicker-free Hero cards in MainActivity and History.
---

# Hero Section UI Stability

This skill documents the layout and animation standards to prevent flickering and layout jitters in high-impact Hero sections.

## Layout Constraints

1. **Width Management:** Hero amount `TextView`s MUST use `android:layout_width="match_parent"` instead of `wrap_content` to prevent the card from recalculating its center or width during numeric transitions.
2. **Line Control:** Always set `android:maxLines="1"` and `android:ellipsize="end"` to guarantee vertical stability.
3. **Auto-Scaling Typography:** Use Material 3 auto-sizing to handle varying currency lengths and massive amounts:
   ```xml
   app:autoSizeTextType="uniform"
   app:autoSizeMinTextSize="24sp"
   app:autoSizeMaxTextSize="45sp"
   ```

## Animation Stability

1. **Decimal Preservation:** During count-up animations, the logic MUST determine if the target value has decimals and maintain that decimal count throughout the animation. Flashing from `120` to `120.4` to `121` causes horizontal jittering and MUST be avoided.
2. **Formatting Parity:** Every frame of an animation must re-apply the **50% smaller symbol** and **standard spacing** through `SpannableStringBuilder` to ensure a smooth, coherent transition.

## Performance Checklist
- [ ] Is the TextView wider than the largest possible value?
- [ ] Does the text shrink to fit without wrapping?
- [ ] Are decimal places stable during animation?
- [ ] Is the currency symbol consistently smaller than the numbers?

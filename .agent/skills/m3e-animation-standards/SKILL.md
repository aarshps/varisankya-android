---
name: Material 3 Expressive Animation Standards
description: Guidelines for implementing M3 Expressive (M3E) animations, transitions, and touch physics in Varisankya.
---

# Material 3 Expressive (M3E) Animation Standards

Varisankya strictly adheres to **Material 3 Expressive** design parameters. Standard M3 is too linear; M3E utilizes dramatic scale depth, sudden entrances with long easing tails, and heavy touch physics.

## Core Duration Tokens

All animations MUST use the predefined M3E tokens in `Constants.kt`. Never use hardcoded millisecond values in Views or XML.

*   `ANIM_DURATION_SHORT` (200ms): Use for rapid snaps, such as a bouncing recovery after an interactive press.
*   `ANIM_DURATION_MEDIUM` (400ms): Use for standard layout state changes.
*   `ANIM_DURATION_LONG` (500ms): Use for **all** Activity/Fragment Shared Axis window transitions and large list entrances. This provides the signature "fast entrance, long Emphasized tail" feel.


## 2. 3D Cascading List Entrances

When loading a list of items (RecyclerView), the items MUST NOT just slide upward. They must cascade from a deeper Z-axis scale.
*   Use `AnimationHelper.animateEntrance(view, position)`.
*   Items must start at `scaleX = 0.85f, scaleY = 0.85f` and animate to `1.0f` while translating up `50f`.
*   This must happen over the `ANIM_DURATION_LONG` (500ms) curve.

## 3. Cinematic Screen Transitions

We use `MaterialSharedAxis(MaterialSharedAxis.Z)` for all primary activity-to-activity navigation (Home -> Settings, Home -> History).
*   **Crucial Rule:** The default Android window animation duration is too short for M3E.
*   You **MUST** override the `.duration` property of the `MaterialSharedAxis` object to `Constants.ANIM_DURATION_LONG` (500ms) on both the enter/return and exit/reenter definitions.

```kotlin
// Correct Implementation in Activity onCreate
window.enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
    duration = Constants.ANIM_DURATION_LONG
}
window.returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
    duration = Constants.ANIM_DURATION_LONG
}
```

## 4. Scroll Harmony & Haptics

M3 Expressive relies on mechanical tactile feedback.
*   Use `PreferenceHelper.attachScrollHaptics(recyclerView)` for standard lists, OR bake the logic directly into `NestedScrollView.setOnScrollChangeListener` if multiple scroll actions (like FAB hiding) need to coexist.
*   **FAB Hiding Rule:** If a `FloatingActionButton` exists over a scrollable list, it **MUST** hide entirely (`.hide()`) when scrolling down, and show (`.show()`) when scrolling up. Do not use Extended FABs for the main feed, as they permanently obstruct too much vertical space.
*   **Edge-to-Edge Padding:** Lists must have a dynamic bottom padding equal to `insets.bottom + FAB resting height + margin` (usually ~88dp total) so the final item in the list can be scrolled *above* the resting FAB.

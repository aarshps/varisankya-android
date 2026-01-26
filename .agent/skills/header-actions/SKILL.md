---
name: Header Action Styling
description: Standards for top-bar action icons, alignment, and tints
---

# Header Action Styling

This skill documents the standardized design for the application's header (Toolbar) actions.

## Action Placement
 
 | Action | Location | implementation |
 |--------|----------|----------------|
 | **History** | **Hero Card** | Tapping the "Remaining Expenses" hero card opens History. |
 | **Add New** | **FAB** | Floating Action Button (Bottom Right) with `colorPrimary`. |
 | **Profile** | **Toolbar** | Top right corner, clearly separated. |
 | **Search** | **Toolbar** | Top left/center (Trigger layout). |

## Toolbar Alignment Standards

### 1. Standard Menu APIs
**Always** use standard Android Menu Items (`onCreateOptionsMenu`) for action buttons (like Logout, Edit, Save). 
- **Why**: Prevents clipping, ensures accessible touch targets (48dp), and handles safe area insets automatically.
- **Do Not**: Inject custom `ConstraintLayout` or `TextView` into the Toolbar unless absolutely necessary for branding.

### 2. Optical Alignment
To achieve pixel-perfect visual alignment between different screen headers:
- **Standard Branding**: Use `app:contentInsetEnd="24dp"` for solid elements (like Profile Images).
- **Action Icons**: Use `app:contentInsetEnd="32dp"` for Menu Items.
  - *Context*: A standard solid image touches the 24dp line. A standard Menu Icon (24dp inside 48dp ripple) has internal padding. Increasing inset to **32dp** nudges the visual icon left, aligning its optical center/edge matches the solid image's presence.
 
## Expressive Motion (M3E)

1. **Logo Shake**: Tapping the Profile/Logo image MUST trigger a premium rotation shake:
   ```kotlin
   view.animate().rotationBy(15f).setDuration(100).withEndAction {
       view.animate().rotationBy(-30f).setDuration(200).withEndAction {
           view.animate().rotation(0f).setDuration(100).start()
       }.start()
   }.start()
   ```

2. **Tactile Spring**: All header trigger layouts (Search, Profile) MUST use `AnimationHelper.applySpringOnTouch()`.

3. **Mechanical Scroll**: Toolbar transparency or elevation changes should coincide with scroll ticks if possible.

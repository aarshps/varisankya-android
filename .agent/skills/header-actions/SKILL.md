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
 
 ## Visual Specs
 
 - **FAB:** Must use `app:backgroundTint="?attr/colorPrimary"` and `app:tint="?attr/colorOnPrimary"` to ensure visibility against the dynamic surface.
 - **Profile:** `ShapeableImageView` with strictly defined dimensions (32dp).
 - **Search:** Rounded pill shape with `selectableItemBackground`.
 
 ## Implementation: activity_main.xml
 
 Use a standard `CoordinatorLayout` setup. The FAB should be anchored to the bottom end.
 
 ```xml
 <com.google.android.material.floatingactionbutton.FloatingActionButton
     android:id="@+id/fab_add_subscription"
     app:backgroundTint="?attr/colorPrimary"
     app:tint="?attr/colorOnPrimary"
     ... />
 ```

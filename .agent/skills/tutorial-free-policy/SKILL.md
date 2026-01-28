---
name: Tutorial-Free Architecture Policy
description: Guidelines for maintaining a clean, distraction-free app state without onboarding tutorials
---

# Tutorial-Free Architecture Policy

Varisankya follows a "Clarity First" design philosophy. Onboarding tutorials, spotlight walkthroughs, and demo modes have been explicitly removed to reduce codebase complexity and prioritize a streamlined "Land and Track" experience for experienced users.

## Core Rules

1. **Self-Documenting UI**: Features must be discoverable through clear M3 labels, icons, and native interactions (e.g., Swipe-to-Action) rather than instruction cards.
2. **No Mock Injection**: The `MainViewModel` and `PreferenceHelper` must never contain code that injects non-user data for "simulated" scenarios.
3. **Empty State Guidance**: Use `empty_state_container` within the Home screen or Search to provide context when no data exists, instead of an overlay.
4. **No Tutorial Artifacts**: Avoid adding `TutorialManager`, `TutorialOverlayFragment`, or any `.json`/`.xml` resources specific to onboarding logic.

## Exception: Empty State CTA
If a user is lost, the only acceptable guidance is an expressive "Zero State" card that leads directly to the primary action (e.g., "Add Subscription").

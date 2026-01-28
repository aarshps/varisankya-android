---
name: Android 15 Standards
description: Guidelines for maintaining strict Android 15 (SDK 35) compliance in Varisankya.
---

# Android 15 Standards (SDK 35+)

Varisankya is a showcase of modern Android development, strictly enforcing `minSdk = 35` (Android 15). This skill outlines the technical and architectural reasons for this decision and the standards it imposes.

## 1. Native Edge-to-Edge Enforcement
Android 15 makes edge-to-edge layout mandatory. Varisankya embraces this proactively:
-   **No Legacy Insets:** We do not handle legacy `fitsSystemWindows`.
-   **Native APIs:** We rely entirely on `WindowCompat.setDecorFitsSystemWindows(window, false)` and `enableEdgeToEdge()` in `BaseActivity`.
-   **Bottom Sheets:** All bottom sheets must handle their own insets to avoid navigation bar overlap.

## 2. Modern Security & Privacy
Targeting SDK 35 allows us to use the strictest security defaults without backward compatibility layers:
-   **Credential Manager:** We use `androidx.credentials` exclusively for authentication (Google Sign-In). Legacy `GoogleSignInClient` is banned.
-   **BiometricPrompt:** We use `androidx.biometric` with strict `BIOMETRIC_STRONG` requirements.
-   **Privacy Sandbox:** We adhere to the latest privacy sandbox limitations for tracking and data access.

## 3. Clean Architecture (No Legacy Support)
The `minSdk 35` requirement is a strategic choice to avoid technical debt:
-   **No AndroidX Compat Shims:** Where native APIs exist in SDK 35, prefer them over Compat wrappers if the wrapper adds significant bulk for older OS versions.
-   **Latest Dependencies:** Always use the alpha/beta versions of Core KTX and Lifecycle libraries that target the newest platform features.

## 4. UI/UX Implications
-   **Haptic Feedback:** Use `HapticFeedbackConstants` introduced in newer APIs (e.g., `CONFIRM`, `REJECT`) freely.
-   **Predictive Back:** Ensure all Views and Activities support the predictive back gesture animation natively.

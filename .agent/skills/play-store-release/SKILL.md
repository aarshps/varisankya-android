---
name: Play Store Release Management
description: Guidelines for managing Varisankya releases across Internal, Closed, and Open tracks.
---

# Play Store Release Strategy

Choosing the right track in the Google Play Console ensures stability and proper audience targeting.

## Track Definitions

### 1. Internal Testing ("Dev Track")
-   **Review:** None (Instant availability).
-   **Audience:** Restricted List (Email invite only). max 100.
-   **Use Case:** Quick sanity checks, verifying "Release" build variants, and testing hotfixes before submission.

### 2. Closed Testing ("Alpha")
-   **Review:** **Mandatory** (1-3 days).
-   **Audience:** Invited Groups (Google Groups/Email lists).
-   **Use Case:** Sharing with specific trusted users/friends for feedback on new features.

### 3. Open Testing ("Beta")
-   **Review:** **Mandatory** (1-3 days).
-   **Audience:** Public (Anyone can join via Store Listing).
-   **Use Case:** Large scale load testing.
-   **UX Warning:** Users must click "Join Beta" -> Wait -> "Install". **Do not use this for marketing launches** (like Product Hunt) as it adds friction.

## Release Hierarchy & Versioning
-   **Production is King:** If a user is eligible for builds in multiple tracks (e.g., Open Testing and Production), they receive the one with the **Highest Version Code**.
-   **Same Version:** You cannot have Version 32 in Open Testing *and* Production simultaneously if they are the exact same build artifact.
    -   If Version 32 is in Production, enabling it for Open Testing is redundant.
    -   To use Open Testing, you must build **Version 33** (or higher).

## Launch Day Protocol
For major external launches (Product Hunt, Press):
1.  **Target Production:** Ensure the stable build is fully rolled out to Production (100%).
2.  **Avoid Beta Friction:** Do not send users to a Testing track link. Give them the direct Production link for instant "Install".

---
name: Play Store Release Management
description: Guidelines for managing Varisankya releases across Beta and Production tracks.
---

# Play Store Release Strategy

Choosing the right track in the Google Play Console ensures stability and proper audience targeting.

## Track Definitions

### 1. Open Testing ("Beta") - [DEFAULT PRE-RELEASE TRACK]
-   **Review:** **Mandatory** (1-3 days).
-   **Audience:** Public (Anyone can join via Store Listing).
-   **Use Case:** Large scale load testing and beta testing. When a "pre-release" is requested, a GitHub pre-release and a Play Store Open Testing (Beta) release should be created in parallel. This is the default target when running `./gradlew publishBundle`.
-   **UX Warning:** Users must click "Join Beta" -> Wait -> "Install". **Do not use this for marketing launches** (like Product Hunt) as it adds friction.

### 2. Production ("Live")
-   **Review:** **Mandatory**.
-   **Audience:** Everyone.
-   **Use Case:** Once a pre-release is tested and approved, the build is promoted to Production on the Play Store, and a corresponding final GitHub release is created.

## Release Hierarchy & Versioning
-   **Production is King:** If a user is eligible for builds in multiple tracks (e.g., Open Testing and Production), they receive the one with the **Highest Version Code**.
-   **Same Version:** You cannot have Version 32 in Open Testing *and* Production simultaneously if they are the exact same build artifact.
    -   If Version 32 is in Production, enabling it for Open Testing is redundant.
    -   To use Open Testing, you must build **Version 33** (or higher).

## Launch Day Protocol
For major external launches (Product Hunt, Press):
1.  **Target Production:** Ensure the stable build is fully rolled out to Production (100%).
2.  **Avoid Beta Friction:** Do not send users to a Testing track link. Give them the direct Production link for instant "Install".

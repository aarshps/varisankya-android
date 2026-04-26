# Cloud Android Development Architecture Plan

## Overview
This document outlines the planned infrastructure and workflow for a high-performance, cost-effective, cloud-based Android development environment. The goal is to move all heavy compilation and deployment tasks to a remote server, enabling development from any machine.

## 1. Cloud Infrastructure (The Server)
**Provider Recommendation:** Hetzner Cloud (or similar high-performance VPS).
*   **Instance Type:** e.g., CPX31 (AMD EPYC)
*   **Specs:** 4 dedicated vCPUs, 8GB RAM, 160GB NVMe SSD.
*   **OS:** Ubuntu 24.04 LTS
*   **Reasoning:** Android builds (Gradle) are heavily bottlenecked by disk I/O and CPU. Hetzner's NVMe drives and dedicated AMD cores provide compile times that rival high-end local workstations at a fraction of AWS/GCP costs (~$14/mo).

## 2. Development Workflow (Headless Remote)
To avoid the lag of VNC/Remote Desktop, the environment will be "headless":
1.  **Local Machine:** Runs VS Code (or JetBrains Gateway).
2.  **Connection:** Connects to the cloud server via Remote-SSH.
3.  **The Experience:** The editor UI runs locally and feels native, but the file system, terminal, Java JDK, Android SDK, and Gradle daemon run entirely on the cloud server.

## 3. Automation & CI/CD Pipeline (Fastlane)
The server will be configured with **Fastlane** to automate the entire build, test, and release lifecycle.

### Phase A: The Testing Loop
1.  Developer commits code or runs a specific Fastlane command (e.g., `fastlane build_debug`).
2.  The server compiles the Debug APK.
3.  Fastlane uses the GitHub API to automatically create a "Pre-Release" tag on this repository.
4.  Fastlane attaches the compiled APK to the GitHub Release.
5.  The developer downloads the APK directly to their physical Android device for testing. *(Note: Hardware-accelerated emulators are generally not supported on cheap VPS providers due to nested virtualization limits).*

### Phase B: The Production Release
1.  When testing is approved, the developer runs `fastlane publish_playstore`.
2.  Fastlane retrieves the securely stored keystore and passwords (from Bitwarden, as documented in `PUBLISHING_SECRETS_RECOVERY.md`).
3.  The server builds the signed Release App Bundle (.aab).
4.  Fastlane uses the Google Play Developer API to automatically upload the bundle, update changelogs, and submit the release to the Play Console.

## 4. Setup Checklist (Next Steps)
- [ ] Provision the Ubuntu server and secure it (SSH keys, firewall).
- [ ] Install OpenJDK, Android Command Line Tools, and accept SDK licenses.
- [ ] Set up the Remote-SSH connection in the local IDE.
- [ ] Install Ruby and Fastlane on the server.
- [ ] Initialize Fastlane in the Android project (`fastlane init`).
- [ ] Write the `Fastfile` lanes for GitHub Releases (using `set_github_release`).
- [ ] Write the `Fastfile` lanes for Play Store uploading (using `upload_to_play_store`).
- [ ] Securely transfer the Bitwarden secrets to the server's environment.
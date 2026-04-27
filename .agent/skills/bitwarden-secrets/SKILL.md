---
name: Bitwarden Build Secrets Recovery
description: Instructions for extracting the Play Store upload keystore, Firebase JSON, and passwords from Bitwarden.
---

# Secrets Recovery Workflow

Varisankya's production signing keys and Firebase API configurations are never committed to version control. They are securely vaulted in Bitwarden under the secure note **"Varisankya"** in the **"Hora"** folder.

## The Automated Extraction Script
To pull the secrets down into the local headless Ubuntu environment, an automated extraction script is provided in the project root: `retrieve_secrets.sh`.

### What it does:
1. Prompts the user to securely unlock Bitwarden (`bw unlock`).
2. Fetches the `Varisankya` secure note.
3. Decodes and merges split Base64 properties to extract `app/google-services.json`.
4. Decodes and merges split Base64 properties to extract `varisankya-upload-key`.
5. Injects the `Key Alias`, `Keystore Password`, and `Key Password` directly into the local (uncommitted) `local.properties` file.

### When to run it:
- If a build fails with `File google-services.json is missing`.
- If a release build fails due to a missing keystore.
- After moving to a fresh environment.

### Usage:
```bash
./retrieve_secrets.sh
```
*(The user will need to press `Tab` to focus into the terminal to provide their Master Password).*
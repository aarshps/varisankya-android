# Publishing Secrets Recovery

The keystore and Firebase configuration required to build and publish the release version of this app are securely stored in Bitwarden.

**Location in Bitwarden:**
* **Folder:** `Hora`
* **Secure Note Name:** `Varisankya`

## Automated Retrieval (Ubuntu CLI / CI)
The project now includes an automated Bash script to securely retrieve and inject these secrets headlessly.
1. Run `./retrieve_secrets.sh` in the project root.
2. Enter your Bitwarden Master Password when prompted.
3. The script will automatically unlock the vault, download the `Varisankya` secure note, decode the split Base64 payloads, and place them in the correct directories.

## Manual Retrieval (Legacy/Windows)
If the automated script cannot be used, you must retrieve the secrets manually.

1. Open Bitwarden and locate the Secure Note mentioned above.
2. Inside the Secure Note, you will find "Custom Fields" containing Base64 encoded strings for the required files (some files may be split into `[Part 1]` and `[Part 2]`).
3. Look for the fields named `varisankya-upload-key`, `google-services.json`, `Key Alias`, `Keystore Password`, and `Key Password`.
4. Copy the Base64 value of each field and decode them.

**Example (macOS/Linux):**
```bash
echo "BASE64_PART_1_AND_2_MERGED" | base64 --decode > app/google-services.json
```

## File Placement
* `varisankya-upload-key`: This is the signing keystore. Place it in the project root or inject the path into `gradle.properties`.
* `google-services.json`: Place it in `app/google-services.json`.
* `local.properties`: Update your local (uncommitted) `local.properties` with:
  ```properties
  RELEASE_STORE_PASSWORD=your_password
  RELEASE_KEY_ALIAS=your_alias
  RELEASE_KEY_PASSWORD=your_password
  RELEASE_STORE_FILE=../varisankya-upload-key
  ```
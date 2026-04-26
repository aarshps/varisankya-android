# Publishing Secrets Recovery

The keystore and Firebase configuration required to build and publish the release version of this app are securely stored in Bitwarden.

**Location in Bitwarden:**
* **Folder:** `Hora`
* **Secure Note Name:** `Varisankya Publishing Secrets`

## How to Retrieve
1. Open Bitwarden and locate the Secure Note mentioned above.
2. Inside the Secure Note, you will find "Custom Fields" containing Base64 encoded strings for the required files.
3. Look for the fields named `varisankya-upload-key`, `google-services.json`, and `credentials.txt`.
4. Copy the Base64 value of each field.
5. Decode the Base64 value into a file using a terminal or an online tool.

**Example (Windows PowerShell):**
```powershell
[IO.File]::WriteAllBytes("varisankya-upload-key", [Convert]::FromBase64String("BASE64_STRING_HERE"))
```

**Example (macOS/Linux):**
```bash
echo "BASE64_STRING_HERE" | base64 --decode > varisankya-upload-key
```

## File Placement
* `varisankya-upload-key`: This is the signing keystore. Previously, it was located at `C:\Users\<YourUser>\varisankya-upload-key`. Ensure the Android Studio "Generate Signed Bundle / APK" dialog points to its extracted location.
* `google-services.json`: Place it in `app/google-services.json`.
* `credentials.txt`: Open this to view the required `Key Alias`, `Keystore Password`, and `Key Password` for signing.
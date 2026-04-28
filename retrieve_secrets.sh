#!/bin/bash
echo "Please enter your Bitwarden Master Password to unlock the vault."
export BW_SESSION=$(bw unlock --raw)

if [ -z "$BW_SESSION" ]; then
    echo "Error: Failed to unlock Bitwarden. Incorrect password or session could not be established."
    exit 1
fi

echo "Vault unlocked successfully. Syncing..."
bw sync

echo "Retrieving Varisankya secure note..."
ITEM_JSON=$(bw get item Varisankya)

echo "Extracting and decoding google-services.json..."
PART1=$(echo "$ITEM_JSON" | jq -r '.fields[] | select(.name=="google-services.json [Part 1]").value')
PART2=$(echo "$ITEM_JSON" | jq -r '.fields[] | select(.name=="google-services.json [Part 2]").value')
echo "${PART1}${PART2}" | base64 --decode > app/google-services.json

echo "Extracting and decoding varisankya-upload-key..."
KPART1=$(echo "$ITEM_JSON" | jq -r '.fields[] | select(.name=="varisankya-upload-key [Part 1]").value')
KPART2=$(echo "$ITEM_JSON" | jq -r '.fields[] | select(.name=="varisankya-upload-key [Part 2]").value')
echo "${KPART1}${KPART2}" | base64 --decode > varisankya-upload-key

echo "Extracting Google Play Console Service Account key..."
echo "$ITEM_JSON" | jq -r '.fields[] | select(.name=="credentials.txt").value' | base64 --decode > app/play_console_key.json

echo "Extracting keystore alias and passwords to gradle.properties..."
# In the JSON structure printed earlier, the alias and passwords were provided directly as separate fields:
# "Key Alias", "Keystore Password", "Key Password"
# So we don't even need to read credentials.txt. Let's just extract them directly!
KEY_ALIAS=$(echo "$ITEM_JSON" | jq -r '.fields[] | select(.name=="Key Alias").value')
KEYSTORE_PASSWORD=$(echo "$ITEM_JSON" | jq -r '.fields[] | select(.name=="Keystore Password").value')
KEY_PASSWORD=$(echo "$ITEM_JSON" | jq -r '.fields[] | select(.name=="Key Password").value')

# Securely append to local.properties (git ignored) if not already present
if ! grep -q "RELEASE_STORE_PASSWORD" local.properties; then
    echo "" >> local.properties
    echo "RELEASE_STORE_PASSWORD=$KEYSTORE_PASSWORD" >> local.properties
    echo "RELEASE_KEY_ALIAS=$KEY_ALIAS" >> local.properties
    echo "RELEASE_KEY_PASSWORD=$KEY_PASSWORD" >> local.properties
    echo "RELEASE_STORE_FILE=../varisankya-upload-key" >> local.properties
fi

echo "Cleaning up..."
# Removed rm -f credentials.txt since it is now play_console_key.json

echo "✅ All secrets successfully retrieved and configured."

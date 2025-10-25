# Keystore Directory

This directory contains the Android keystore for signing release builds.

## Security Notice
- **Never commit keystore files (*.jks, *.p12) or passwords to git**
- These files are ignored by `.gitignore` for security
- Keystore and passwords are stored as GitHub Actions secrets

## Files (ignored by git)
- `app-release.jks` - PKCS#12 keystore for signing
- `app-release.jks.base64` - Base64 encoded for GitHub secrets
- `keystore-password.txt` - Temporary password file (delete after use)

## GitHub Secrets Required
Upload these secrets to your repository for CI/CD signing:
- `ANDROID_KEYSTORE_BASE64` - Base64 content of the keystore
- `ANDROID_KEYSTORE_PASSWORD` - Keystore password
- `ANDROID_KEYSTORE_ALIAS` - Key alias (globaltranslation)

## Usage in GitHub Actions
The keystore is decoded from the base64 secret at build time and used for signing release builds.
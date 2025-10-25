# Keystore Directory

This directory contains the Android keystore for signing release builds.

## Current Configuration (Testing Mode)
**The app is currently configured to build WITHOUT requiring a keystore.**

- Release builds will create **unsigned APKs** if no keystore is present
- This allows testing and debugging without keystore management
- The app will use the Android debug keystore if available at `~/.android/debug.keystore`
- Perfect for development, testing, and CI/CD environments

## Security Notice
- **Never commit keystore files (*.jks, *.p12) or passwords to git**
- These files are ignored by `.gitignore` for security
- Keystore and passwords should be stored as GitHub Actions secrets when needed

## Adding a Production Keystore (Future)
When you're ready to publish signed releases:

1. **Generate a release keystore:**
   ```bash
   keytool -genkey -v -keystore app-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias globaltranslation
   ```

2. **Update `app/build.gradle.kts`:**
   - Modify the `signingConfigs` section to use your production keystore
   - Store credentials in environment variables or GitHub secrets

3. **For GitHub Actions CI/CD:**
   - Encode keystore: `base64 app-release.jks > app-release.jks.base64`
   - Upload these secrets to your repository:
     - `ANDROID_KEYSTORE_BASE64` - Base64 content of the keystore
     - `ANDROID_KEYSTORE_PASSWORD` - Keystore password
     - `ANDROID_KEYSTORE_ALIAS` - Key alias (globaltranslation)

## Files (ignored by git)
- `app-release.jks` - PKCS#12 keystore for signing
- `app-release.jks.base64` - Base64 encoded for GitHub secrets
- `keystore-password.txt` - Temporary password file (delete after use)
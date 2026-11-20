# Building and updating KW Player

## Debug APK for testing

Every push to `main`, pull request to `main`, or manual workflow dispatch starts **Android build**.

When the workflow succeeds:
1. Open the repository's Actions tab.
2. Open the latest **Android build** run.
3. Download the `KWPlayer-debug` artifact and extract the APK.
4. Install it on a test device.

Debug APKs are for testing only. They are not suitable for publishing updates to an already installed release build.

## One-time release signing setup

Release APKs are signed with one permanent private keystore. Never commit the keystore or its passwords to this public repository. Losing the keystore means future APKs cannot update installations signed with the old key.

Create a JKS keystore once using Android Studio's **Build > Generate Signed Bundle / APK > APK > Create new**. Choose a long unique password and alias, save the JKS file in a safe private location, and keep a backup.

Add these four repository Actions secrets in **Settings > Secrets and variables > Actions**:

- `KEYSTORE_BASE64` — Base64 content of the JKS file.
- `KEYSTORE_PASSWORD` — keystore password.
- `KEY_ALIAS` — key alias.
- `KEY_PASSWORD` — key password.

Encode the JKS file as a single Base64 string before adding `KEYSTORE_BASE64`. Do not paste the keystore or passwords into chat, issues, commits, or workflow logs.

## Publish an update

1. Increase `versionCode` and `versionName` in `app/build.gradle.kts` for every release. The version code must always increase.
2. Commit the changes to `main`.
3. Create and push a tag matching the version, for example `v0.2.0`.
4. **Signed Android release** builds the APK and publishes it as `KWPlayer.apk` in GitHub Releases.
5. In the app, open **Обновления** → **Проверить обновления** → **Скачать обновление** → **Установить обновление**.

The in-app updater reads the latest public GitHub Release and uses Android's standard package installer. Android may ask once for permission to install apps from KW Player.

## Important migration note

The old CI artifact was a debug-signed APK. Android does not allow a release-signed APK to update an installation signed with a different key. The first transition from that debug build to the permanent release key requires uninstalling the debug app and installing the first signed release. After that, updates can be installed over the existing app without deleting its data, provided the same keystore is retained.

## Real-device testing

Test on POCO X8 Pro Max and POCO M8 5G:
- Install the signed APK.
- Publish a higher version and verify the in-app update check.
- Download and install the update without uninstalling the app.
- Confirm media playback and stored app data remain intact.

A successful CI build proves compilation only. It does not prove playback compatibility, battery efficiency, or thermal performance; those require real-device testing.

# Building and testing

## Automated debug APK

Every push to `main`, pull request to `main`, or manual workflow dispatch starts the **Android build** GitHub Actions workflow.

When the job succeeds:
1. Open the repository's Actions tab.
2. Open the latest **Android build** run.
3. Download the `ModernMediaPlayer-debug` artifact.
4. Extract the APK and install it on an Android test device.

The first build may take longer while Gradle and Android dependencies are downloaded.

## Local Android Studio build

Open the repository root as an Android project in a recent Android Studio release with JDK 17 and Android SDK Platform 36 installed. The current CI workflow uses Gradle 8.13. A Gradle wrapper will be added when its official wrapper JAR and scripts can be committed and verified; do not invent or substitute a wrapper binary.

## First device test

On each target phone:
- Install the debug APK.
- Open a locally stored MP4/H.264/AAC file.
- Verify playback, pause, seeking, rotation, and returning to the app.
- Test an MKV file and record its actual video/audio codecs.
- Test a file with unsupported codecs and confirm the app shows a useful error rather than crashing.
- Check that playback pauses when the app is backgrounded.
- Note device model, Android version, file container, video codec/profile, audio codec, resolution, and result.

A successful CI build proves compilation only. It does not prove playback compatibility, battery efficiency, or thermal performance; those require real-device measurements.

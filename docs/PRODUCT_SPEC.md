# Product specification

## Product vision

KW Player (Kibato Watch Player) is a free, local-first Android media player with a Winamp Modern-inspired interface and VLC-style practical playback.

## Principles

1. Stable playback and battery efficiency first.
2. Media stays on the device. No accounts, ads, analytics, telemetry or cloud upload.
3. Network access is limited to the optional GitHub release checker and APK download.
4. No codec support is claimed without physical-device testing.
5. Skins are safe presentation presets and never execute imported code.

## Implemented foundation

- Kotlin, Jetpack Compose Material 3 and AndroidX Media3.
- Playback owned by MediaSessionService; audio focus and noisy-headset handling are configured through ExoPlayer.
- Open local audio/video files and select folders through SAF.
- Explicit recursive scan of user-selected folders.
- Video/audio lists, filename search, sorting, favorites and recently opened history.
- Room database stores the scanned media index and resume positions; legacy SharedPreferences index remains available for migration/fallback.
- Preferences DataStore stores skin and accent settings and migrates the former skin preference.
- Dedicated audio now-playing overlay with seek, shuffle, previous/next, playback toggle and device-supported equalizer bands.
- Dedicated landscape video player with Media3 PlayerView, seek, ±10 second controls, audio/subtitle track selection dialogs, aspect-ratio modes, repeat, shuffle and selectable playback speeds.
- Optional background playback for video, disabled by default and switchable both in the video player and settings. When disabled, video pauses as the app enters the background.
- Built-in Modern Dark, Classic Orange, Light and AMOLED Black theme choices.
- Glance playback widget with previous/play-pause/next controls.
- In-app GitHub release check, APK download and installer handoff.

## Not yet complete or device-verified

- Full metadata artwork and true 24-band spectrum visualization. Android Visualizer requires an explicit RECORD_AUDIO permission flow; no fabricated spectrum animation is shown.
- Full subtitle file picker, audio-track picker, aspect-ratio controls, lock-screen touch lock and brightness/volume gestures.
- Folder widget configuration and folder-content widget layouts.
- MediaStore-wide discovery outside user-selected SAF folders.
- Editable persistent playlists, complete queue editor and per-item queue persistence.
- Resume-position behavior across firmware/process-kill scenarios.
- Decoder/codec compatibility matrix, battery/thermal measurements and HyperOS notification/widget validation.

## Privacy and storage

Library, favorites, history, settings and playback positions are local. Scanning is user-triggered. No permanent media polling or background directory watcher is used.

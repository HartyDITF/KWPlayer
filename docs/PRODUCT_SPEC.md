# Product specification

## Product vision

KW Player (Kibato Watch Player) is a free, local-first Android media player with a polished, skin-based interface and practical video/audio controls inspired by established players.

## Principles

1. Stable playback and battery efficiency first.
2. Media stays on the device. No accounts, ads, analytics, telemetry or cloud upload.
3. Network access is limited to the optional GitHub release checker and APK download.
4. No codec support is claimed without physical-device testing.
5. Skins are safe presentation presets and never execute imported code.

## Implemented foundation

- Kotlin, Jetpack Compose Material 3 and AndroidX Media3.
- Playback owned by MediaSessionService; ExoPlayer handles audio focus and noisy-headset events.
- Open local audio/video files and select folders through SAF.
- Explicit recursive scanning of user-selected folders, with Room media index and a legacy local index fallback.
- Video/audio library views with search, sorting, favorites and recently opened history.
- Release SAF folder permissions when a folder is removed from the library.
- Preferences DataStore stores the selected skin, accent and background-video playback preference.
- Dedicated audio now-playing screen with seek, shuffle, previous/next, queue preview and device-supported equalizer bands.
- Dedicated video player with seek, ±10-second controls, playback speed, aspect-ratio modes, audio/subtitle track selection, external subtitle file loading, repeat and shuffle.
- Video touch lock, brightness/volume gestures, fullscreen handling and Picture-in-Picture entry.
- Optional background playback for video, disabled by default and switchable both in the video player and settings. When disabled, video pauses as the app enters the background.
- Five built-in visual styles: KW Modern, Winamp Modern, Spotify Style, One UI and AMOLED Neon.
- Glance playback widget with previous/play-pause/next controls.
- Android MediaSession notification/lock-screen integration through Media3.
- In-app GitHub release check, APK download and installer handoff.

## Not yet complete or device-verified

- Full metadata/artwork coverage across providers and true audio spectrum visualization. Android Visualizer requires an explicit RECORD_AUDIO permission flow; no fabricated spectrum animation is shown.
- Subtitle appearance controls and advanced subtitle/audio delay controls.
- Folder widget configuration and folder-content widget layouts; multiple independent folder widgets are not yet complete.
- MediaStore-wide discovery outside user-selected SAF folders.
- Editable persistent playlists and a full queue editor with per-item persistence.
- Resume-position behavior across firmware/process-kill scenarios.
- Decoder/codec compatibility matrix, battery/thermal measurements and HyperOS notification/widget validation.
- Physical-device verification of all playback controls, PiP, background playback and long-session stability.

## Privacy and storage

Library, favorites, history, settings and playback positions are local. Scanning is user-triggered. No permanent media polling or background directory watcher is used.

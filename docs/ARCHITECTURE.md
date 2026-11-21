# KW Player architecture

## Goals

Keep playback independent from presentation, minimize idle work and APK size, and make file access explicit and user-controlled.

## Technology

| Area | Implementation |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Playback | AndroidX Media3 ExoPlayer, MediaSessionService and MediaController |
| File access | Storage Access Framework (SAF) |
| Library index | Lightweight JSON records in SharedPreferences (Room is not integrated yet) |
| Settings | SharedPreferences |
| CI | GitHub Actions |

## Playback

PlaybackService owns ExoPlayer and MediaSession. The Activity connects through MediaController. Media3 handles playback and system media controls. The Activity does not own the playback engine.

The service should be active only for an active playback session. Do not start it for idle library browsing.

## SAF library

- Users add folders with ACTION_OPEN_DOCUMENT_TREE.
- Persist the granted URI permission; never assume absolute filesystem paths.
- Scanning is started only by an explicit user action.
- MediaLibrary walks selected document trees through DocumentsContract and indexes audio/video MIME types.
- The local index stores URI, display name, MIME type, size and modified time.
- Favorites and the most recent 100 opened items are stored locally.
- If access is revoked or a provider fails, skip the unavailable entry and continue where possible.

The current lightweight index uses SharedPreferences JSON. Room migration, duration metadata and per-file resume positions are future work; do not describe them as implemented.

## UI and skins

The app has Video, Audio, Browse, Playlist and More sections. Standard uses bottom navigation; Winamp Modern and Classic Retro use top navigation. Skin presets also alter panel shape and density. Playback logic is shared across skins.

Skins are built-in safe UI presets. Do not load or execute arbitrary code from skin files.

## Performance and privacy

- No background folder watcher or periodic media scan.
- No service while idle.
- Do not load complete media files into memory.
- No analytics, ads, accounts or media uploads.
- Artwork and thumbnails must use bounded caches if added later.

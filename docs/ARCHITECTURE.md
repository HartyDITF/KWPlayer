# KW Player architecture

## Goals

Keep playback independent from presentation, minimize idle work and APK size, and make file access explicit and user-controlled.

## Technology

| Area | Implementation |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Playback | AndroidX Media3 ExoPlayer, MediaSessionService and MediaController |
| File access | Storage Access Framework (SAF), with MediaStore discovery planned |
| Library index | Room (migration from the existing SharedPreferences JSON index is pending) |
| Settings | Preferences DataStore (migration from existing SharedPreferences is pending) |
| Widgets | Jetpack Glance AppWidget (widget implementations are pending) |
| CI | GitHub Actions |

## Playback

PlaybackService owns ExoPlayer and MediaSession. The Activity connects through MediaController. Media3 handles playback and system media controls. The Activity does not own the playback engine.

The service should be active only for an active playback session. Do not start it for idle library browsing.

## SAF library

- Users add folders with ACTION_OPEN_DOCUMENT_TREE.
- Persist the granted URI permission; never assume absolute filesystem paths.
- Scanning is started only by an explicit user action.
- MediaLibrary walks selected document trees through DocumentsContract and indexes audio/video MIME types.
- The current index stores URI, display name, MIME type, size and modified time in SharedPreferences JSON.
- Favorites and the most recent 100 opened items are stored locally.
- If access is revoked or a provider fails, skip the unavailable entry and continue where possible.
- Room entities and migration must be implemented before claiming Room-backed persistence.

## UI and skins

The design token foundation lives in core/design/theme. The default palette follows the KW Player dark palette and uses orange as the initial accent. Saved accent selection and complete screen migration are pending.

Skins are built-in safe UI presets. Do not load or execute arbitrary code from skin files.

## Performance and privacy

- No background folder watcher or periodic media scan.
- No service while idle.
- Do not load complete media files into memory.
- No analytics, ads, accounts or media uploads.
- Network access is reserved for the optional GitHub release checker and APK download.
- Artwork and thumbnails must use bounded caches if added later.

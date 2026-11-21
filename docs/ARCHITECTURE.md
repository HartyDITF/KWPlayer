# Initial architecture

## Architecture goals

Keep the default installation small, avoid unnecessary native dependencies, isolate playback from presentation, and make background behavior explicit.

## Main technology choices

| Area | Choice | Reason |
|---|---|---|
| Language | Kotlin | Native Android development |
| UI | Jetpack Compose | Reusable, themeable interface |
| Playback | AndroidX Media3 ExoPlayer + MediaSessionService | Platform playback, media notification and system controls |
| Storage access | Storage Access Framework | User-granted access to chosen files and folders |
| Local index | Room (planned) | Structured media records and resume positions |
| Preferences | SharedPreferences for current skin preference; DataStore remains planned | Keep the first implementation lightweight |
| Widgets | Android App Widgets (planned) | Home-screen integration with event-driven updates |
| CI | GitHub Actions | Reproducible APK builds and test automation |

## Playback service

PlaybackService owns the ExoPlayer instance and MediaSession. The Activity connects through a MediaController and does not own or release the playback engine. Playback can continue when the Activity is no longer visible. Media3 publishes the session through Android's media notification integration, allowing system media controls and compatible HyperOS surfaces to discover playback state.

The service is active only for an active playback session. It must not be started for idle browsing. The Activity releases its controller connection when destroyed; the service owns player lifetime.

## Folder permission and library flow

1. User selects a folder using ACTION_OPEN_DOCUMENT_TREE.
2. App takes the persistable URI permission returned by Android.
3. Store the folder URI and a user-friendly label locally.
4. Scan only after an explicit user action or an explicitly enabled schedule.
5. Store media URI, display name, duration, media type, and playback position in Room.
6. Re-check access when opening a file; gracefully handle revoked permissions or moved files.

Do not store assumptions about absolute filesystem paths. SAF document URIs are the source of truth for selected folders.

## Widget behavior

- Each widget instance stores its own selected folder identifier or playback-control configuration.
- Opening a folder launches the app at that folder.
- Playback controls send explicit actions to the playback session.
- Widget refreshes are triggered by user actions and relevant playback state changes.
- No tight timer or frequent polling loop.
- Keep widget layouts compact and avoid loading large artwork synchronously.

## Theme / skin system

The app uses a standard dark interface by default. Built-in color presets are selected in Settings and are independent from playback: Standard, Winamp Modern, AMOLED Black, Classic Retro, and Neon. The current lightweight preset selection is stored locally. A future skin system may add validated data-only layouts and assets; skins must never execute arbitrary code.

## Battery and memory safeguards

- No service when idle.
- Foreground playback service only while playback is active and media controls are required.
- Release ExoPlayer and surfaces promptly after the playback session ends.
- Hardware decoding by default.
- No continuous folder watcher or recurring widget polling.
- Lazy-load artwork and thumbnails with bounded caches.
- Avoid expensive blur, particles, spectrum effects, and idle redraws.

## Licensing and distribution

Track every direct and transitive dependency and its license before release. Media3 is the playback choice. Any additional engine requires a separate APK-size, performance, and licensing review.

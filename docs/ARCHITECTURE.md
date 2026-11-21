# KW Player architecture

## Technology

| Area | Implementation |
|---|---|
| UI | Kotlin, Jetpack Compose, Material 3 |
| Playback | AndroidX Media3 ExoPlayer, MediaSessionService, MediaController |
| Local file access | Storage Access Framework |
| Library index | Room |
| Settings | Preferences DataStore with SharedPreferences migration |
| Home screen | Jetpack Glance player widget |
| CI | GitHub Actions |

## Playback ownership

PlaybackService owns ExoPlayer and MediaSession. The Activity connects through MediaController. ExoPlayer is configured to manage audio focus and react to audio becoming noisy (for example, wired headphones being disconnected). The AndroidX Media3 session service provides the system media notification and lock-screen transport controls.

The service owns the player lifecycle. The UI never creates a second player. The equalizer attaches to the player's audio session and uses the band capabilities exposed by the device.

## Library and persistence

- Users grant folder access through ACTION_OPEN_DOCUMENT_TREE.
- Persistable URI permissions are retained; absolute filesystem paths are not assumed.
- Scanning starts only after explicit user action.
- SAF traversal is bounded by a maximum depth and visited-document set.
- Room stores URI, display name, MIME type, size, modified time and resume position.
- The existing JSON SharedPreferences index remains as a migration/fallback source for older installs.
- Favorites and history continue to use the existing local preferences until their Room migration is completed.
- Unavailable or revoked document providers are skipped without aborting the full scan.

## Settings and skins

Preferences DataStore migrates the existing skin preference and stores the selected skin and accent color. Built-in skins are safe presentation presets. Imported skin files must never execute code.

## Widgets

The Glance player widget sends explicit local broadcasts to a receiver that connects to the MediaSession and issues transport controls. Widget updates are event-driven; there is no periodic polling.

## Performance and privacy

- No background folder watcher or periodic scan.
- No player instance while browsing; the MediaSessionService owns playback.
- Media files are streamed by Media3, not loaded fully into RAM.
- No analytics, ads, accounts, telemetry or media uploads.
- Network access is reserved for GitHub Releases update checks and APK downloads.

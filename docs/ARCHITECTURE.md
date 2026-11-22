# KW Player — Architecture

## Direction

Treat this as a product-level redesign, not a sequence of cosmetic patches. Replace the current monolithic screen composition with feature-oriented Compose screens and a small set of shared domain/data services. Keep one playback engine and one playback session.

## Layers

- **app**: activity, navigation, app lifecycle and dependency wiring.
- **core/design**: design tokens, theme definitions, reusable controls, typography and adaptive system-bar behavior.
- **core/playback**: Media3 player/session facade, queue model, track selection, playback preferences and audio-route integration.
- **data/media**: SAF folder grants, explicit media indexing, metadata/artwork access, Room entities/DAOs and repositories.
- **data/settings**: DataStore-backed theme, player, gesture, library and privacy preferences.
- **feature/video**: video library, grid/list presentation, video player and in-player settings.
- **feature/music**: songs/albums/artists/folders, playlists and now-playing UI.
- **feature/folders**: SAF folder browser and per-widget folder configuration.
- **feature/widgets**: folder widgets and playback widget.
- **feature/settings**: appearance, playback, library, subtitles, audio and privacy settings.
- **feature/downloads**: optional download queue and resumable transfer state, isolated from playback.

## Playback ownership

One Media3 ExoPlayer instance is owned by a MediaSessionService. UI screens attach/detach controllers; they do not create competing players. The service owns audio focus, noisy-route handling, MediaSession, notification and persistence of playback position. The background-video-audio preference defaults to false and is applied when the activity/player screen leaves the foreground; changing it in the player updates the same session.

PiP is an Activity presentation of the existing player/session, not a second player. Releasing controllers must not release the service-owned player.

## Storage and indexing

Use SAF and persisted URI permissions. Do not request broad storage permissions for ordinary user-selected folders. Scans are explicit and bounded; do not run a permanent watcher or periodic scanner. Handle provider exceptions, revoked grants and null metadata. Room is the source of indexed media metadata and playback position; DataStore is the source of preferences. Avoid blocking database or document-provider operations on the main thread.

## UI and theming

Use a shared design-token model. Screens consume tokens rather than embedding skin-specific colors. Each theme maps to the same semantic tokens. Keep navigation and playback behavior independent from theme selection. System dynamic colors are optional and gated by Android version. Use WindowInsets APIs and edge-to-edge layout intentionally; never hardcode status/navigation bar colors without matching icon contrast.

## Widgets

Each widget instance stores its own configuration keyed by GlanceId/appWidgetId, including the selected SAF URI. Do not store one global folder selection. Update widgets from explicit configuration changes, media updates and playback events. Widget clicks use explicit PendingIntent/Glance actions and open a validated URI in the app.

## Downloads

Download workers are separate from playback and use persistent WorkManager jobs only when the feature is approved and dependencies are justified. Persist URL identity, validators, bytes written, target URI/path and state. Use Range/If-Range where supported; verify response status and Content-Range before appending. Write to a temporary file and atomically finalize where possible. Network loss pauses/retries without discarding verified partial data. Never append blindly after a server returns a full 200 response. Treat signed URLs as expiring secrets and do not log them.

## Performance and reliability

- Hardware decoding via Media3 by default.
- No full media file reads into RAM.
- Bounded thumbnail/artwork cache.
- No permanent polling loops.
- Coroutine dispatchers for I/O and provider operations.
- Lifecycle-safe listeners/controllers and deterministic release.
- Catch recoverable provider/decoder errors at feature boundaries; surface actionable messages.
- Do not add Rust or a second playback engine without profiling that demonstrates a concrete bottleneck.

## Validation

CI compilation and automated tests are necessary but insufficient. Device test matrix and scenarios are maintained in PRODUCT_SPEC.md and ROADMAP.md. Release only after build artifacts and CI status are verified; label untested device behavior honestly.

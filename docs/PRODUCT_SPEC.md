# KW Player — Product Specification

## Product direction

KW Player (Kibato Watch Player) is a free, local-first Android media player. The product is designed as a new cohesive experience inspired by the navigation patterns and playback capabilities of VLC, YouTube and YouTube Music/Spotify. It must not copy proprietary assets, logos, source code or exact branded artwork.

## Non-negotiable principles

- Offline-first for local media; no accounts, ads, analytics, telemetry or mandatory servers.
- Kotlin, Jetpack Compose, AndroidX Media3, SAF, Room, DataStore and Glance remain the default stack.
- Android hardware decoding is preferred. No full-file buffering in memory.
- Avoid persistent polling, unnecessary services, excessive animations and unbounded caches.
- A feature is not complete because it compiles; device validation is required.
- Keep minSdk 23, compileSdk 36 and Java 17 unless a documented compatibility reason requires a change.

## App navigation

Primary destinations:
1. Video
2. Music
3. Folders
4. Playlists
5. History
6. Favorites

Settings are opened from the app bar gear action. Search, sorting, filters and view mode are available in relevant libraries. Video library supports a home-feed card grid and a compact list mode. Both modes must show correct title, artwork/thumbnail, duration and available metadata, with graceful fallbacks when metadata is absent.

## Video library

- User-selected folders and individual files through Storage Access Framework.
- Persist URI permissions and handle revoked permissions without crashes.
- User-triggered scanning only; no continuous storage scan.
- Grid/list view, search, sort, filters, favorites, history and resume position.
- Metadata and thumbnails loaded asynchronously with bounded caches.
- Folder widgets can be configured independently and open a selected item in KW Player.

## Video playback

- Media3-backed local video playback.
- Full-screen and edge-to-edge layouts with correct Android system bars and cutout handling.
- Standard transport controls, seek bar, previous/next, playback speed, repeat and queue.
- Gestures for seeking, brightness and volume with an option to disable each gesture.
- Aspect-ratio and scaling modes: fit, fill/crop, stretch and original.
- Configurable side bars: black, blurred/derived artwork color, or user-selected solid color. Must not alter video pixels or force expensive frame analysis.
- In-player settings sheet for speed, aspect ratio, screen orientation, gestures, subtitles, audio tracks, background playback and display options.
- Subtitle selection for embedded tracks and external SRT/WebVTT/ASS/SSA where supported by Media3 and device decoders. Report unsupported formats accurately.
- Audio track selection and decoder/format information where exposed by Media3.
- Screen lock/control lock.
- Background video audio playback is user-controlled and OFF by default; can be toggled from the player while a video is open.
- Picture-in-Picture, media notification and lock-screen controls.
- Audio focus and noisy-route handling; allow system output switching through Android's supported route picker. Never promise arbitrary forced routing where Android blocks it.

## Music library and player

- Local audio library with artwork, album, artist, genre and duration metadata where available.
- Browse songs, albums, artists, folders and playlists.
- Search, sorting, favorites, history, queue, shuffle, repeat and resume.
- YouTube Music/Spotify-inspired now-playing layout with artwork, queue and transport controls; VLC-level playback controls and supported audio settings.
- MediaSession notification, lock-screen controls, headset buttons, audio focus and Bluetooth/wired route changes.
- Equalizer is exposed only where supported by the device audio session; failures are non-fatal.

## Themes and appearance

Built-in themes:
- KW Modern (default): contemporary media-library layout combining video feed cards and VLC-like navigation.
- Winamp Modern: dedicated retro-futuristic metallic/blue skin.
- Spotify-inspired: dark music-first surfaces and artwork-led player.
- One UI-inspired: spacious rounded controls and adaptive surfaces.
- AMOLED Neon: true black backgrounds with restrained accent lighting.

Every theme uses shared design tokens and can customize accent color, background/surface colors, card radius, control shape, density and optional effects. Support Android light/dark mode and Material You dynamic colors where available. Themes must not contain executable code and must not alter playback logic. Provide reduced-motion/effects-off options.

## Widgets

- Multiple independent folder widgets, each configured with its own persisted SAF tree URI.
- Widget opens the selected folder or a chosen media item directly in KW Player.
- Playback control widget remains separate from folder widgets.
- Refresh on user actions and relevant app events, not continuous polling.
- Handle deleted folders, revoked permissions and missing media with a recoverable empty state.

## Downloads

An optional, separately permissioned download manager may accept a user-provided supported media URL. For any supported source, persist task state and partial file metadata, use HTTP Range only when the server advertises/accepts byte ranges, validate ETag/Last-Modified or equivalent validators, and resume only when the remote representation is unchanged. If the source provides expiring URLs, refresh through a lawful supported integration. If the server does not support range requests, clearly explain that a restart may be required.

A Rezka-specific integration is not assumed to be stable or authorized. Do not bypass DRM, paywalls, authentication, geo restrictions or other access controls. Do not redistribute copyrighted content. Before implementation, assess site terms, applicable law, source stability and third-party GPL-3.0 obligations. Do not copy HDrezka Grabber source into this project without complying with GPL-3.0 and reviewing compatibility with KW Player's distribution model.

## Quality gates

- CI: assemble debug APK, unit tests, lint/static checks where configured.
- No claiming device validation from CI results.
- Device matrix: POCO X8 Pro Max (primary) and POCO M8 5G (compatibility).
- Test launch, SAF access, grid/list, playback, seek, rotation, PiP, background toggle, lock screen, audio routes, subtitles, track selection, widgets, process recreation, long playback, thermal behavior and battery impact.

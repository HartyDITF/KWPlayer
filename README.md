# ModernMediaPlayer

A private, offline-first Android media player inspired by Winamp Modern and VLC.

## Goals

- Play local video and audio with broad, device-aware format compatibility.
- Keep playback efficient on both entry-level and high-end Android phones.
- Offer multiple themes and skins, including a Winamp Modern-inspired skin.
- Let users pin selected folders to configurable Android home-screen widgets.
- Work fully offline, without accounts, ads, analytics, subscriptions, or a required server.

## Development status

**First app scaffold committed.** The app now has a native Android entry point, a dark starter screen, local video picking, and Media3 playback controls. GitHub Actions is configured to build a debug APK. The first successful CI build and real-device playback tests are still pending.

See:
- [Product specification](docs/PRODUCT_SPEC.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)

## Target devices

Initial real-device testing:
- POCO X8 Pro Max
- POCO M8 5G

The app should also remain usable on lower-memory Android devices. Exact compatibility and performance targets will be established through physical-device testing.

## Technical direction

- Kotlin
- Android Jetpack / Compose for the application UI
- AndroidX Media3 as the default playback engine
- Android Storage Access Framework for user-selected folders
- Room for the local media index and playback positions
- DataStore for preferences
- Android App Widgets for folder shortcuts and playback controls

A second playback engine such as LibVLC will only be considered after compatibility tests demonstrate a meaningful gap. This avoids shipping a large native engine unnecessarily and keeps licensing obligations explicit.

## Privacy

All media indexing, playback history, and preferences are stored locally. The app will not upload media metadata or usage data.

## Build

Build instructions will be added with the first compilable Android project scaffold.

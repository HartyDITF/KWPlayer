# KW Player (Kibato Watch Player)

A free, local-first Android audio and video player built with Kotlin, Jetpack Compose and AndroidX Media3. The interface includes several switchable visual styles, including a Winamp Modern-inspired skin.

## Current foundation

- Local audio and video playback through Media3 and a shared MediaSessionService.
- User-selected SAF folders, manual library scanning, search, sorting, favorites and history.
- Video controls for seeking, playback speed, aspect ratio, audio/subtitle tracks, external subtitle files, repeat, shuffle, touch lock, brightness and volume gestures, and Picture-in-Picture.
- Optional background audio playback for video. It is off by default and can be changed in the video player or settings.
- Audio now-playing screen with queue view, transport controls and device-supported equalizer controls.
- Five built-in visual styles: KW Modern, Winamp Modern, Spotify Style, One UI and AMOLED Neon.
- Android playback notification and a playback home-screen widget.
- Local settings and media index, plus optional GitHub release checking and APK download.

Some features remain incomplete, and device behavior has not yet been validated on all target phones. A successful GitHub Actions build confirms compilation, not real-device playback or codec compatibility.

## Development status

Development is active on main. Every code change is checked through GitHub Actions before it is considered build-verified.

See:
- [Product specification](docs/PRODUCT_SPEC.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Roadmap](docs/ROADMAP.md)
- [Build instructions](docs/BUILDING.md)

## Target devices

Initial real-device testing:
- POCO X8 Pro Max
- POCO M8 5G

The app should remain usable on lower-memory Android devices. Exact codec compatibility, battery use and thermal behavior require physical-device testing.

## Technical direction

- Kotlin and Jetpack Compose
- AndroidX Media3 / ExoPlayer
- Storage Access Framework (SAF)
- Room for the local media index and playback positions
- DataStore for preferences
- Android App Widgets / Jetpack Glance
- GitHub Actions for APK builds

A second playback engine such as LibVLC will only be considered after real-device tests demonstrate a meaningful compatibility gap. This avoids increasing APK size and memory use without evidence.

## Privacy

Media indexing, playback history, favorites and preferences stay on the device. Scanning is user-triggered. The app does not include ads, analytics, accounts, telemetry or media uploads. Network access is used only for the optional GitHub update checker and APK download.

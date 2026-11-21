# Product specification

## Product vision

KW Player (Kibato Watch Player) is a free, local-first Android media player with practical library navigation and selectable interface layouts.

## Principles

1. Stable playback and battery efficiency first.
2. Local media stays on the device.
3. Do not claim codec support without real-device testing.
4. Keep the default interface familiar and functional.
5. Skins change layout and component presentation, not just colors.

## Implemented foundation

- Media3 playback through a MediaSessionService.
- Open individual audio/video files through Android's document picker.
- Add folders through SAF and explicitly scan selected folders.
- Separate Video and Audio library lists.
- Search by filename, sort by name or modified date.
- Favorites and recently opened history.
- Current Media3 queue display.
- Playback speed control.
- In-app GitHub release check, APK download and installer handoff.
- Standard bottom navigation; Winamp Modern and Classic Retro top navigation.
- Built-in Standard, Winamp Modern, AMOLED Black, Classic Retro and Neon appearance presets with distinct panel shapes/density.

The library index currently uses local SharedPreferences JSON. Scanning is user-triggered and only covers folders selected by the user.

## Still planned / not device-verified

- Room database and per-file resume positions.
- Saved editable playlists and queue management.
- Subtitle file selection and audio/subtitle track picker UI.
- Picture-in-picture, fullscreen/orientation polish and gesture controls.
- Thumbnail generation and rich metadata.
- Home-screen widgets.
- Device/codec compatibility matrix and battery/thermal measurements.

## Privacy

No ads, analytics, telemetry, accounts or upload of media names and playback history. The optional updater connects to GitHub Releases.

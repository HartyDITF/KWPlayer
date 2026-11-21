# Product specification

## Product vision

KW Player (Kibato Watch Player) is a free, local-first Android media player with a familiar, straightforward interface. The default interface is neutral and practical; Winamp Modern is an optional appearance selected in Settings, not the default navigation or layout.

## Product principles

1. Battery and performance first.
2. Local-first playback; core playback does not require internet.
3. Broad compatibility, honestly described.
4. Simple by default; advanced controls remain optional.
5. Appearance never changes playback behavior.

## Playback
- Local video and audio playback through Android's Storage Access Framework.
- Media3 ExoPlayer with hardware decoding preferred when supported.
- MediaSessionService-backed playback and Android media notification controls.
- Compatible system surfaces, including HyperOS media surfaces, can discover the media session; exact presentation is controlled by device firmware.
- Playback speed, audio/subtitle selection, gestures, resume, and PiP remain planned until implemented and tested.
- Unsupported formats must produce a clear error.

## Library and folders
- User-selected folders through SAF.
- Persist folder access and list local media.
- User-triggered scanning; no constant recursive scanning.
- Browse video, audio, favorites, history, search and sorting (planned).

## Themes
Built-in appearance presets: Standard (default), Winamp Modern, AMOLED Black, Classic Retro, and Neon. Theme selection is independent of playback. A full data-only skin architecture remains planned.

## Widgets
- Multiple independently configured folder widgets.
- Playback controls widget.
- Event-driven updates without frequent polling.
- These remain planned until implemented and device-tested.

## Privacy
No ads, analytics, accounts, telemetry, or upload of media names and playback history. The updater is the only optional network-dependent feature.

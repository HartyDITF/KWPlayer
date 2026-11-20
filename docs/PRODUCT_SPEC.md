# Product specification

## Product vision

Create a free, personal-use Android media player combining VLC-like playback compatibility and controls with the visual personality and skin flexibility of Winamp Modern.

The first users are the owner and one family member. The first validation devices are POCO X8 Pro Max and POCO M8 5G.

## Product principles

1. **Battery and performance first.** Avoid unnecessary background work, polling, animations, scans, and software decoding.
2. **Local-first.** Playback and the media library must work without an internet connection.
3. **Broad compatibility, honestly described.** A container extension alone does not guarantee support: the contained audio/video codecs, profiles, device decoders, and subtitles also matter.
4. **Simple by default.** Advanced controls should not make ordinary playback complicated.
5. **Skins must not control playback.** Changing appearance must never change decoder selection, playback quality, or media scanning behavior.

## Functional requirements

### Playback
- Local video and audio playback.
- Common containers and codecs, with a compatibility matrix maintained from real tests.
- Hardware decoding preferred whenever Android exposes a suitable decoder.
- Playback position remembered per media item.
- Resume playback and continue to the next item.
- Playback speed controls.
- Audio track selection where supported.
- Embedded and external subtitle selection where supported.
- Subtitle formats to validate include SRT, WebVTT, and ASS/SSA.
- Picture-in-picture where supported by Android.
- Video gestures: seek, brightness, and volume, with adjustable sensitivity.
- Audio playback controls and notification/media-session integration.
- Clear error messages when a codec or file cannot be decoded.

### Library and folders
- User chooses folders explicitly through Android's Storage Access Framework.
- Persist the granted folder permission across restarts.
- User-triggered scans by default; no constant recursive scanning.
- Browse folders, video, audio, favorites, and recently played items.
- Search and basic sorting.
- Multiple selected folders may be managed independently.
- Handle removed, renamed, or inaccessible files without crashing.

### Home-screen widgets
- Multiple widget instances, each configured independently.
- A widget can be linked to a selected folder.
- Folder widget can open that folder in the app.
- Playback widget can show current media and basic controls.
- Combined folder + playback widget may be offered if it remains readable at small sizes.
- Widgets update on user actions and relevant playback/library events; no frequent polling.

### Themes and skins
Initial built-in appearances:
- Winamp Modern-inspired metallic blue/silver.
- AMOLED Black.
- Classic Retro.
- Neon.

Requirements:
- Theme selection independent of player engine.
- Light/dark support where it makes sense.
- Reduced-motion / animations-off option.
- AMOLED theme uses true black backgrounds for major surfaces.
- Skin assets and layout configuration must be versioned and validated.
- No arbitrary executable code loaded from skin packs.

### Settings
- Playback engine and decoder diagnostics.
- Hardware-decoding preference (automatic by default).
- Scan folders and scan behavior.
- Default theme and reduced-motion preference.
- Subtitle appearance and gesture sensitivity.
- Resume behavior and playback history controls.
- Clear local library/history data.

## Non-functional requirements

### Efficiency
- No analytics, ads, telemetry, account system, or required network access.
- Do not run a permanent background service when nothing is playing.
- Release player and decoder resources promptly after playback ends.
- Prefer hardware decoding; do not silently fall back to expensive software video decoding.
- Do not continuously redraw idle screens or widgets.
- Avoid periodic widget refresh loops.
- Keep library scans bounded and user-initiated initially.
- Avoid loading full media files into memory.
- Avoid expensive blur, particle, spectrum, or waveform effects by default.
- Support reduced motion and low-memory behavior.

### Reliability
- Gracefully handle unsupported formats, corrupt files, missing permissions, storage removal, and decoder failures.
- Preserve library metadata and playback positions across app restarts.
- Never assume every device supports every codec or profile.

### Privacy
- No network requests are required for core functionality.
- No media filenames, paths, artwork, or playback history leave the device.
- Permissions are requested only when the corresponding feature is used.

## Initial compatibility baseline

Media3 supports Android API 23+ for core audio/video playback. The initial minimum SDK target will be API 23 unless implementation or device testing identifies a compelling reason to raise it. This is a compatibility baseline, not a guarantee that every feature works on every API-23 device.

## Out of scope for the first release

- Cloud synchronization.
- User accounts.
- Streaming service integrations.
- Network shares and casting.
- Online metadata scraping.
- Automatic background media crawling.
- User-executable skin scripts.

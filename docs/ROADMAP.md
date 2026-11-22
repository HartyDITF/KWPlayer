# Development roadmap

A successful CI build proves compilation only. Device behavior must be tested separately.

## Foundation and playback
- [x] Kotlin / Compose project and GitHub Actions debug APK build.
- [x] Media3 ExoPlayer through MediaSessionService and MediaController.
- [x] Audio focus and noisy-headset handling.
- [x] Local file open and SAF folder selection.
- [x] User-triggered recursive scan, search, sorting, favorites and history.
- [x] Room media index and resume-position storage foundation.
- [x] Preferences DataStore with migration from the previous skin preference.
- [x] Audio now-playing screen with transport controls, seek, queue preview and device-supported equalizer.
- [x] Video screen with Media3 PlayerView, seek, skip and speed controls.
- [x] External subtitle file picker and audio/subtitle track selection.
- [x] Video aspect-ratio modes, repeat, shuffle and touch lock.
- [x] Brightness/volume gestures and Picture-in-Picture entry.
- [x] Optional background audio playback for video, off by default and switchable in-player.
- [x] Glance playback widget with transport controls.
- [ ] Confirm notification and lock-screen controls on POCO X8 Pro Max and POCO M8 5G.
- [ ] Verify resume positions after force-stop, process death and firmware background restrictions.
- [ ] Verify audio focus and headset disconnect behavior on devices.

## Library and player quality
- [x] Video library presentation modes: thumbnail-first home feed, compact list and two-column grid.
- [ ] MediaStore discovery and broader metadata/artwork coverage.
- [ ] Full queue editor, saved playlists and shuffle/repeat persistence.
- [ ] Subtitle styling and audio/subtitle delay controls.
- [ ] True audio spectrum with explicit permission flow.
- [ ] Device/codec compatibility test matrix.

## Design and widgets
- [x] Central dark design tokens and accent color.
- [x] KW Modern, Winamp Modern, Spotify Style, One UI and AMOLED Neon skin presets.
- [ ] Finish typography assets and per-skin layout refinement.
- [ ] Safe skin import format.
- [ ] Folder widgets and configurable independent folder widget setup.
- [ ] Validate widget resize behavior and event-driven refresh.

## Release quality
- [x] GitHub release checker and signed APK installer handoff.
- [ ] Build and publish a signed release APK for the next stable version.
- [ ] Physical-device playback, battery, thermal and long-session testing.

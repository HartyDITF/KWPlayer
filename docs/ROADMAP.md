# Development roadmap

A successful CI build proves compilation only. Device behavior must be tested separately.

## Foundation and playback
- [x] Kotlin / Compose project and GitHub Actions build.
- [x] Media3 ExoPlayer through MediaSessionService and MediaController.
- [x] Audio focus and noisy-headset handling.
- [x] Local file open and SAF folder selection.
- [x] User-triggered recursive scan, search, sorting, favorites and history.
- [x] Room media index and resume-position storage foundation.
- [x] Preferences DataStore with migration from the previous skin preference.
- [x] Audio now-playing screen with transport controls, seek and device-supported equalizer.
- [x] Landscape video screen with Media3 PlayerView, seek, skip and speed controls.
- [x] Glance playback widget with transport controls.
- [ ] Confirm notification and lock-screen controls on POCO X8 Pro Max and POCO M8 5G.
- [ ] Verify resume positions after force-stop, process death and firmware background restrictions.
- [ ] Verify audio focus and headset disconnect behavior on devices.

## Library and player quality
- [ ] MediaStore discovery and metadata/artwork.
- [ ] Full queue editor, saved playlists, shuffle/repeat persistence.
- [ ] Subtitle file picker, audio/subtitle track selection and subtitle styling.
- [ ] Picture-in-picture, video aspect-ratio menu, touch lock and brightness/volume gestures.
- [ ] True audio spectrum with explicit permission flow.
- [ ] Device/codec compatibility test matrix.

## Design and widgets
- [x] Central dark design tokens and accent color.
- [x] Modern Dark, Classic Orange, Light and AMOLED Black theme choices.
- [ ] Finish typography assets (Inter and Share Tech Mono) and per-skin layout refinement.
- [ ] Safe skin import format.
- [ ] Folder widgets and configurable folder widget setup.
- [ ] Validate widget resize behavior and event-driven refresh.

## Release quality
- [x] GitHub release checker and signed APK installer handoff.
- [ ] Build signed release APK after debug CI passes.
- [ ] Physical-device playback, battery, thermal and long-session testing.

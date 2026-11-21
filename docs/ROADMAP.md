# Development roadmap

Status labels: **Not started**, **In progress**, **Blocked**, **Done**. A checked implementation item is not a claim of successful physical-device testing.

## Stage 0 — Foundation
- [x] Kotlin / Compose project and GitHub Actions build.
- [x] Media3 playback baseline.
- [x] Central KW Player dark design-token palette.
- [x] Add Room, Preferences DataStore and Glance dependencies.

## Stage 1 — Playback foundation
- [x] Open a local audio or video file through SAF.
- [x] MediaSessionService and Android media controls integration.
- [ ] Verify background playback and HyperOS media surfaces on devices.
- [ ] Fullscreen/orientation handling and gesture controls.
- [ ] Codec test matrix and useful decoder diagnostics.
- [ ] Queue, shuffle, repeat and resume positions.

## Stage 2 — Library and navigation
- [x] Video, Audio, Browse and Playlist sections.
- [x] Dedicated settings screen opened from the header gear button.
- [x] Edge-to-edge layout with status-bar and navigation-bar safe areas.
- [x] User-selected SAF folders with persisted access.
- [x] Explicit recursive scan of selected folders.
- [x] Local video/audio index with filename search and sorting.
- [x] Favorites and recently opened history.
- [ ] MediaStore discovery.
- [ ] Room entities, DAO, database and migration from the existing local index.
- [ ] Saved playlists and full queue editing.

## Stage 3 — Playback quality
- [x] Playback speed control.
- [ ] Subtitle file selection and audio/subtitle track selection UI.
- [ ] Picture-in-picture.
- [ ] Fullscreen controls and gestures.
- [ ] Verify behavior across common containers/codecs on real devices.

## Stage 4 — Skins
- [x] Existing Standard, Winamp Modern, AMOLED Black, Classic Retro and Neon presets.
- [x] Centralized base color tokens.
- [ ] Inter / Share Tech Mono font assets and typography setup.
- [ ] Accent color persistence with DataStore.
- [ ] Refine skin-specific player controls and accessibility.
- [ ] Safe skin import format and reduced-motion option.

## Stage 5 — Widgets and efficiency
- [ ] Folder shortcut widgets.
- [ ] Playback control widget.
- [ ] Event-driven widget refresh.
- [ ] Validate POCO X8 Pro Max and POCO M8 5G.
- [ ] Measure battery, memory, CPU and thermals.

## Stage 6 — Release
- [x] Signed v0.3.0 release and in-app updater foundation.
- [ ] Build and test the next signed release.
- [ ] Record physical-device test results and known limitations.

## Release rule

CI success proves compilation only. It does not prove playback quality, firmware integration, codec compatibility or battery behavior.

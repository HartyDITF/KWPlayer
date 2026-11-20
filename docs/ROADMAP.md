# Development roadmap

Status labels: **Not started**, **In progress**, **Blocked**, **Done**.

## Stage 0 — Project foundation
- [x] Create private GitHub repository.
- [x] Record product goals and non-functional requirements.
- [x] Record initial architecture and playback-engine strategy.
- [ ] Add Android Gradle project scaffold.
- [ ] Add GitHub Actions debug APK build.
- [ ] Confirm dependency licenses and versions.
- [ ] Establish baseline APK size and build checks.

## Stage 1 — Minimal working player
- [ ] App launches on Android API 23+.
- [ ] Open a single local video through Android file picker.
- [ ] Play, pause, seek, rotate, and enter/exit fullscreen.
- [ ] Handle orientation and lifecycle changes.
- [ ] Release playback resources correctly.
- [ ] Show useful unsupported-file and permission errors.

## Stage 2 — Library and playback quality
- [ ] Add user-selected folders through SAF.
- [ ] Persist folder access and list media.
- [ ] Add Room library and per-file resume position.
- [ ] Add audio playback and media session controls.
- [ ] Add subtitle and audio-track selection.
- [ ] Add gesture controls and configurable sensitivity.
- [ ] Add Picture-in-Picture where supported.

## Stage 3 — Widgets
- [ ] Folder shortcut widget.
- [ ] Independent configuration for multiple widget instances.
- [ ] Playback controls widget.
- [ ] Event-driven refresh; no frequent polling.
- [ ] Validate widget behavior across launchers and screen sizes.

## Stage 4 — Themes and skins
- [ ] Theme tokens and skin data model.
- [ ] Winamp Modern-inspired skin.
- [ ] AMOLED Black.
- [ ] Classic Retro.
- [ ] Neon.
- [ ] Reduced-motion mode.
- [ ] Validate skin packs and safe fallback behavior.

## Stage 5 — Compatibility and efficiency
- [ ] Build a format/codec/subtitle test matrix.
- [ ] Test on POCO X8 Pro Max.
- [ ] Test on POCO M8 5G.
- [ ] Test a lower-memory or older Android device/emulator.
- [ ] Measure battery use during repeatable playback sessions.
- [ ] Measure memory, CPU, thermal behavior, and APK size.
- [ ] Evaluate whether a second playback engine is justified.
- [ ] Fix regressions before adding optional effects.

## Stage 6 — Personal release
- [ ] Produce signed release APK.
- [ ] Document installation and update process.
- [ ] Record known device/codec limitations.
- [ ] Keep source and release artifacts in GitHub.

## Release rule

Do not mark a feature complete because it compiles. It must be tested on a real device where practical. Do not claim "supports all formats"; publish only verified compatibility results.

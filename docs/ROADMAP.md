# Development roadmap

Status labels: **Not started**, **In progress**, **Blocked**, **Done**.

## Stage 0 — Project foundation
- [x] Android project scaffold and GitHub Actions debug build.
- [x] Media3 dependency baseline.

## Stage 1 — Playback foundation
- [x] Open a local video or audio file through Android's document picker.
- [x] Media3 playback session separated from Activity lifetime.
- [x] Android media notification / MediaSession controls integration.
- [ ] Real-device verification of background playback, notification controls and HyperOS surfaces.
- [ ] Fullscreen viewing, orientation behavior and playback gestures.
- [ ] Clear decoder diagnostics and format test matrix.

## Stage 2 — Library and playback quality
- [ ] User-selected folders through SAF and media browsing.
- [ ] Room library and per-file resume position.
- [ ] Subtitle and audio-track selection.
- [ ] Picture-in-picture and playback speed controls.

## Stage 3 — Widgets
- [ ] Folder shortcut widget.
- [ ] Independent configuration for multiple widget instances.
- [ ] Playback controls widget.
- [ ] Event-driven refresh; no frequent polling.
- [ ] Validate widget behavior across launchers and screen sizes.

## Stage 4 — Themes and skins
- [x] Initial local appearance presets: Standard, Winamp Modern, AMOLED Black, Classic Retro, Neon.
- [ ] Full theme-token and data-only skin architecture.
- [ ] Reduced-motion setting and skin validation.

## Stage 5 — Compatibility and efficiency
- [ ] Test on POCO X8 Pro Max and POCO M8 5G.
- [ ] Test a lower-memory or older Android device.
- [ ] Measure battery, memory, CPU and thermal behavior.
- [ ] Evaluate whether a second playback engine is justified.

## Stage 6 — Personal release
- [ ] Produce signed release APK.
- [ ] Document installation and update process.
- [ ] Record known device/codec limitations.

## Release rule

A successful build does not prove real-device behavior. Do not claim support for a codec or firmware integration until it has been tested.

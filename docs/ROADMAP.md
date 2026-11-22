# KW Player — Roadmap

This roadmap describes the complete target, not a claim that these features are already implemented.

## Target release scope

The intended redesign is delivered as one coherent application experience. Work may be split into internal commits to keep changes reviewable and builds diagnosable; the user should not need to approve each subsystem separately.

- [ ] New app navigation and KW Modern visual system.
- [ ] Video library with grid/list, metadata, artwork, search, sort, favorites and history.
- [ ] Full video player with VLC-inspired controls and YouTube-inspired in-player settings.
- [ ] Side-field modes: black, artwork-derived/blurred and custom solid color.
- [ ] Subtitle and audio-track selection, speed, scaling, gestures and control lock.
- [ ] Background video audio toggle (default OFF), MediaSession, lock-screen controls and PiP.
- [ ] Music library, playlists, queue and artwork-led now-playing experience.
- [ ] Five themes: KW Modern, Winamp Modern, Spotify-inspired, One UI-inspired and AMOLED Neon.
- [ ] Per-theme color, shape, density and effects customization; Android system/dynamic colors.
- [ ] Independent SAF folder widgets plus playback widget.
- [ ] Optional resumable download manager after source/legal/terms review.
- [ ] Automated tests, lint/static analysis, CI APK and device validation.

## Engineering sequence

1. Establish new design tokens, navigation and feature boundaries.
2. Rebuild video and music libraries around the same media repository.
3. Integrate video and audio screens with one Media3 session.
4. Implement the complete in-player control/settings model.
5. Add theme presets and per-theme customization.
6. Implement independent folder widget configuration.
7. Evaluate and implement a lawful, stable download source with validated HTTP resume.
8. Run CI, fix all build/static/test failures, then perform physical-device testing.

## Release gates

- [ ] GitHub Actions build succeeds for the exact candidate commit.
- [ ] Unit/instrumentation/static checks pass where available.
- [ ] APK artifact is present and downloadable.
- [ ] No known crash or data-loss issue remains in tested flows.
- [ ] POCO X8 Pro Max and POCO M8 5G testing results are recorded separately.
- [ ] Untested features are not advertised as verified.

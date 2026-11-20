# Initial architecture

## Architecture goals

Keep the default installation small, avoid unnecessary native dependencies, isolate playback from presentation, and make background behavior explicit.

## Main technology choices

| Area | Initial choice | Reason |
|---|---|---|
| Language | Kotlin | Native Android development |
| UI | Jetpack Compose | Reusable, themeable interface |
| Playback | AndroidX Media3 ExoPlayer | Native Android media integration and platform decoder access |
| Storage access | Storage Access Framework | User-granted access to chosen folders |
| Local index | Room | Structured media records and resume positions |
| Preferences | DataStore | Small, asynchronous settings storage |
| Widgets | Android App Widgets; choose Glance or RemoteViews during implementation | Home-screen integration with event-driven updates |
| Background work | WorkManager only for deferrable, user-requested work | Avoid persistent services and unnecessary wakeups |
| CI | GitHub Actions | Reproducible APK builds and test automation |

Dependency versions will be selected from current stable releases when the Android project scaffold is created. Do not pin guessed or preview versions in this planning document.

## Playback engine strategy

### Phase 1: Media3 only

Media3 is the default engine. It uses Android platform decoders by default, so the actual supported sample formats depend on the device. Container support and codec support are separate concerns.

The player should:
- Prefer hardware decoders when suitable.
- Report decoder capabilities and playback failures clearly.
- Release resources when the playback session ends.
- Avoid automatic software video decoding if it would cause excessive heat or battery use.
- Keep playback state independent from UI theme state.

### Phase 2: Compatibility evaluation

Create a real-device test matrix covering common containers, video codecs, audio codecs, subtitle types, seeking, and high-resolution files.

Only if important files fail on target devices should an additional engine be evaluated. LibVLC is a candidate, but it adds native binaries, APK size, maintenance, and licensing considerations. It must not be included merely for a theoretical promise of "all formats".

If a second engine is added, isolate it behind a PlaybackEngine interface and make fallback behavior explicit and testable. Do not switch engines repeatedly or silently during a single playback session.

## Suggested module boundaries

The initial implementation may begin as a small number of Gradle modules. Do not split into many modules prematurely.

- app: application entry point, navigation, dependency wiring.
- core: shared models and interfaces.
- playback: Media3 integration, playback service/session, decoder diagnostics.
- library: SAF folder grants, scanning, Room entities and repositories.
- widgets: widget configuration and event-driven updates.
- design: theme tokens, components, skin definitions.
- testing: media fixtures and device compatibility test documentation.

These are logical boundaries; create physical Gradle modules only when they provide clear value.

## Folder permission and library flow

1. User selects a folder using ACTION_OPEN_DOCUMENT_TREE.
2. App takes the persistable URI permission returned by Android.
3. Store the folder URI and a user-friendly label locally.
4. Scan only after an explicit user action or an explicitly enabled schedule.
5. Store media URI, display name, duration, media type, and playback position in Room.
6. Re-check access when opening a file; gracefully handle revoked permissions or moved files.

Do not store assumptions about absolute filesystem paths. SAF document URIs are the source of truth for selected folders.

## Widget behavior

- Each widget instance stores its own selected folder identifier or playback-control configuration.
- Opening a folder launches the app at that folder.
- Playback controls send explicit actions to the playback session.
- Widget refreshes are triggered by relevant user actions and playback state changes.
- No tight timer or frequent polling loop.
- Keep widget layouts compact and avoid loading large artwork synchronously.

## Theme / skin system

Separate design tokens from playback code.

A skin definition may configure:
- colors and gradients;
- typography choices from bundled/system fonts;
- corner radii, borders, and spacing;
- button and panel styles;
- optional decorative assets.

Skin packs must be data-only (for example, validated JSON plus image assets). Do not execute scripts or arbitrary code from a skin package. Invalid or unsupported skin definitions must fall back to the default theme.

Animations and audio visualizers are opt-in, bounded, and disabled or paused when not visible. The AMOLED theme should use true black for major backgrounds.

## Battery and memory safeguards

- No service when idle.
- Foreground playback service only when required for ongoing audio playback and media controls.
- Release ExoPlayer and surfaces promptly.
- Hardware decoding by default.
- Do not run expensive visualizers by default.
- No continuous folder watcher.
- No recurring widget polling.
- Lazy-load artwork and thumbnails with bounded caches.
- Avoid decoding full-resolution artwork for small UI elements.
- Test long playback sessions for thermal behavior and memory growth.

## Licensing and distribution

Track every direct and transitive dependency and its license before adding it.

Media3 is the initial playback choice. If LibVLC or FFmpeg components are introduced, review their exact Android packaging, enabled codecs, binary distribution terms, and corresponding source/license obligations before shipping an APK. Personal use does not remove the need to understand the licenses of redistributed binaries.

## References

- Android Media3 supported formats: https://developer.android.com/media/media3/exoplayer/supported-formats
- Android Media3 supported devices: https://developer.android.com/media/media3/exoplayer/supported-devices
- Android app widget update guidance: https://developer.android.com/develop/ui/views/appwidgets/advanced
- Official VLC Android project and LibVLC information: https://github.com/videolan/vlc-android

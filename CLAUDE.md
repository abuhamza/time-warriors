# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

TimewGUI is a Compose Multiplatform desktop GUI for the [Timewarrior](https://timewarrior.net/) CLI (`timew`). It provides visual time tracking while preserving full CLI data compatibility. The sidebar branding is "TimeTrackAI".

- **Kotlin 2.1.20** / **Compose Multiplatform 1.10.1** (Desktop) / **JDK 21**
- Root package: `com.timewgui`, entry point: `Main.kt`

## Build & Run Commands

The Makefile defaults `JAVA_HOME` to the macOS Homebrew path (`?=`), override with env var on Linux.

```bash
make run              # Run in foreground
make build            # Full build (compile + jar)
make compile          # Compile only (~12s incremental, ~1.5min full)
make clean            # Clean build artifacts
make package-dmg      # Package macOS .dmg
make package-deb      # Package Linux .deb
make ship v=X.Y.Z m="msg"  # Commit, push, and release
```

Or directly: `./gradlew run`, `./gradlew build`, `./gradlew compileKotlin`

Tests live in `src/test/kotlin/` using JUnit 5, MockK, and kotlinx-coroutines-test. Run with `make test` or `./gradlew test`.

## Dev Feedback Loop (UI Verification)

After UI-affecting changes, verify visually:

```bash
make dev-feedback     # Full cycle: stop → run-bg → wait-window → screenshot
```

Or step by step: `make run-bg` → `make wait-window` → `make screenshot` → read `screenshots/latest.png`

First-time macOS setup: grant Screen Recording permission to the calling app via System Settings → Privacy & Security → Screen Recording.

## Architecture

### MVVM with CLI-as-Backend

The GUI **never** reads/writes Timewarrior's `.data` files directly. All data flows through `timew` CLI commands:

```
AppState (owns TimewCli singleton, navigation, settings)
  ├── TimerViewModel(timewCli, onError)
  ├── TimelineViewModel(timewCli, onError)
  ├── TagViewModel(timewCli)
  ├── IdleViewModel(timewCli, timerViewModel, appState, onError)
  ├── TaskViewModel(taskRepository)
  └── OvertimeViewModel(timewCli, onError)
```

### Critical Rules

1. **All `timew` interactions go through `TimewCli.kt`** — never call `timew` directly from UI code
2. **All CLI methods return `Result<T>`** — always handle both `onSuccess` and `onFailure`
3. **After any mutation** (delete, modify, tag, etc.), call `refreshIntervals()` on `TimelineViewModel`
4. **`AppState` owns the `TimewCli` singleton** and passes it to ViewModels via constructor injection
5. ViewModels use `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`; CLI calls are `suspend` on `Dispatchers.IO`

### Source Layout

- `domain/` — CLI wrapper (`TimewCli.kt`), data models (`Interval.kt`, `TagInfo.kt`, `Task.kt`), idle detection, launch-at-login, recurrence engine
- `domain/api/` — External integrations (absence.io client, Hawk auth)
- `domain/repository/` — Data persistence (`TaskRepository`)
- `viewmodel/` — `AppState`, `TimerViewModel`, `TimelineViewModel`, `TagViewModel`, `IdleViewModel`
- `ui/theme/` — "African Savanna" color palette, typography, dimensions
- `ui/components/` — reusable composables (Sidebar, TopBar, Timeline, TagSelector, etc.)
- `ui/screens/` — Dashboard, Timeline, Reports, Tags, Tasks, Settings
- `ui/navigation/Screen.kt` — screen enum for routing

## Theme & Styling

The design language is **"African Savanna"** — warm earth tones, not neutral gray/blue.

- Use `LocalTimewColors.current` (or `TimewTheme.colors`) for custom tokens — do NOT use `MaterialTheme.colorScheme` for custom tokens
- Times/durations **must** use `TimewTypography.monospace`
- Tag colors: 8 earth-tone `TagColorPair` slots in `Color.kt`, persisted via `java.util.prefs.Preferences`
- Timeline blocks use 0px border radius (sharp corners) — intentional
- Dashboard banner is a Canvas drawing (gradient sky, sun, acacia trees) — not an image

### Key Dimensions

| Constant | Value |
|---|---|
| `sidebarWidth` / collapsed | 200.dp / 56.dp |
| `borderRadiusCard` | 12.dp |
| `borderRadiusTimeline` | 0.dp |
| `bannerHeight` | 180.dp |

## Known Gotchas

- `kotlinx.datetime.Instant` is deprecated — use `kotlin.time.Instant` for new code
- `Icons.Outlined.Label` is deprecated — use `Icons.AutoMirrored.Outlined.Label`
- First macOS launch after full rebuild may crash with MetalRedrawer error — just run again
- `timew` must be installed and on PATH
- Idle detection and Launch at Login are macOS-only
- Settings are persisted via `java.util.prefs.Preferences` (separate from `~/.timewarrior/timewarrior.cfg`)

## Design Preferences

- Information density is valued — avoid unnecessary whitespace or decorative elements
- Canvas-based rendering preferred for timeline visualizations
- Warmth and earth tones over clinical precision

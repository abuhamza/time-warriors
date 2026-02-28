# System Patterns

## Architecture Pattern
- **MVVM-like**: ViewModels hold state via Compose `mutableStateOf`, composables observe
- **CLI-as-backend**: All time-tracking data mutations go through `timew` CLI commands
- **Single source of truth**: Timewarrior's data files are authoritative; GUI reads via `timew export`
- **Dual persistence**: Time data via `timew` CLI; tasks via `TaskRepository` (JSON file); GUI settings via `java.util.prefs.Preferences`

## Key Design Decisions
1. **No direct file access**: GUI never reads/writes `.data` files; always uses CLI
2. **Reactive state**: Compose `mutableStateOf`/`mutableStateListOf` for UI reactivity
3. **Coroutine-based CLI**: All CLI calls are `suspend` functions on `Dispatchers.IO`
4. **Result types**: CLI methods return `Result<T>` for error handling
5. **Tag color persistence**: Stored via `java.util.prefs.Preferences`, separate from timew config
6. **African savanna theme**: Custom "Warm Savanna" color palette (warm earth tones, amber accent) instead of the PRD's neutral palette. Includes a Canvas-based savanna sunset gradient banner on the dashboard.
7. **Settings persistence**: All GUI-specific settings (idle threshold, launch at login, tag colors) use `java.util.prefs.Preferences`, keeping them separate from `timewarrior.cfg`
8. **Idle detection via ioreg**: No JNA dependency — uses macOS `ioreg -c IOHIDSystem` to parse `HIDIdleTime` (nanoseconds); macOS-only
9. **Launch at Login via plist**: Creates/removes `~/Library/LaunchAgents/com.timewgui.plist`; macOS-only
10. **Task persistence via JSON file**: Tasks stored at `~/.config/timewgui/tasks.json` using `kotlinx-serialization`. Separate from both `timew` data and `java.util.prefs.Preferences`. Tasks link to intervals via generated tags (`task:<slug>`).
11. **Card text tokens for dark mode**: Dedicated `textOnCardPrimary/Secondary/Tertiary` and `borderOnCard` tokens ensure contrast on cream card surface (#FFF5E8) in dark mode

## Component Relationships
- `AppState` owns the `TimewCli` singleton, navigation state, sidebar state, theme preference, and idle/launch settings (persisted via Preferences)
- ViewModels receive `TimewCli` via constructor injection
- `TaskViewModel` uses `TaskRepository` (no CLI dependency — tasks are app-managed)
- `IdleViewModel` additionally receives `TimerViewModel` and `AppState` to check running state and settings
- Screens compose UI components and connect them to ViewModels
- `Main.kt` creates all ViewModels (including `TaskViewModel` and `IdleViewModel`) and routes screens via `when` on `Screen` enum
- `TimewGuiTheme` provides both Material3 `ColorScheme` and custom `TimewColors` via `CompositionLocalProvider`

## Theme Architecture
- `TimewColors` data class extends beyond Material3 with custom tokens: `bgPrimary/Secondary/Tertiary`, `accent`, `success`, `destructive`, `warning`, `cardSurface`, `textOnCardPrimary/Secondary/Tertiary`, `borderOnCard`
- `TagColorPair` holds light/dark variants for each of the 8 tag color slots
- Accessed via `LocalTimewColors.current` or `TimewTheme.colors`
- `TimewDimensions` holds layout constants: sidebar widths, border radii, banner height
- `TimewSpacing` provides 4px base grid unit

## Design Language
- Warm African savanna aesthetic with earth tones (amber, rust, forest green, deep brown)
- Sidebar branded "TimeTrackAI" with timer icon
- Sharp corners on timeline blocks (precision tool aesthetic)
- Monospace font for all times and durations
- No shadows, no gradients on UI chrome (gradient only on dashboard banner)
- Material3 as foundation, mapped to savanna color tokens
- 8-slot earth-tone tag color palette (rust, forest, amber, mauve, teal, berry, olive, burnt sienna)

## Screen Layout
- Main layout: Sidebar (left, collapsible) + Column (TopBar + active screen)
- TopBar: persistent timer display with green pulsing dot, start/stop controls
- StartTimerDialog: modal overlay for tag selection and optional backdated start
- IdleDialog: modal AlertDialog with Keep Tracking / Pause & Resume / Stop Timer buttons
- IntervalDetailPanel: slide-in from right for editing interval details
- SnackbarHost: error display at bottom

## Navigation
- `Screen` enum: DASHBOARD, TIMELINE, REPORTS, TAGS, TASKS, SETTINGS
- Sidebar with icon + label navigation items (Tasks uses `Icons.Outlined.CheckBox`)
- Sidebar expand/collapse with chevron toggle at bottom

## Task Management Architecture
- `Task` has lifecycle states: TODO → IN_PROGRESS → DONE → ARCHIVED
- `TaskRepository` handles CRUD + file-based persistence to `~/.config/timewgui/tasks.json`
- `TaskRepository.generateTag(title)` creates slugified tags like `task:fix-login-bug` for linking to `timew` intervals
- `TaskViewModel` exposes task list and CRUD actions to `TasksScreen`
- Tasks are deliberately separate from Timewarrior to avoid tag proliferation (evaluated in session 1bce6974)

## Dev Feedback Loop Pattern
- **Instance stamping**: Each `make run-bg` generates a unique epoch ID, passes it to Gradle as `-PinstanceId=<id>`, which the build script forwards as a JVM arg `-Dtimewgui.instanceId=<id>` — visible in the process command line for `pgrep -f` detection
- **PID tracking**: The detected JVM PID is written to `.dev/app.pid`; `make stop` kills only that specific process
- **Quartz window detection**: `scripts/dev-screenshot.sh` uses `pyobjc-framework-Quartz` (auto-installed in `.dev/venv/`) to find the app window by `kCGWindowOwnerPID` (layer-0 only), falling back to title match then full-screen capture
- **Permission-aware**: Checks `screencapture` permission before attempting capture; auto-opens System Settings with clear instructions on failure
- **Cursor rule**: `.cursor/rules/dev-feedback-loop.mdc` triggers on `src/main/kotlin/**/*.kt` edits, teaching the agent the full workflow

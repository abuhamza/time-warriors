# Progress

## Completed
- [x] Project scaffolding (Gradle 8.12, Kotlin 2.1.20, Compose MP 1.10.1)
- [x] Makefile with convenience targets (run, build, compile, clean, package-*)
- [x] Domain models (Interval with custom serialization, TagInfo, Task with TaskStatus)
- [x] CLI integration (TimewCli with all timew commands via ProcessBuilder)
- [x] Task management (Task model, TaskRepository with JSON persistence, TaskViewModel, TasksScreen)
- [x] UI Theme — African savanna color palette (light + dark modes, 8 tag color slots)
- [x] Custom theme tokens: TimewColors (including card-specific text tokens), TimewDimensions, TimewSpacing
- [x] ViewModels (AppState, TimerViewModel, TimelineViewModel, TagViewModel, IdleViewModel, TaskViewModel)
- [x] Core UI components (Sidebar, TopBar, Timeline, IntervalDetailPanel, IntervalList, TagSelector, StartTimerDialog, ProgressBar, IdleDialog)
- [x] All 6 screens (Dashboard, Timeline, Reports, Tags, Tasks, Settings)
- [x] Dashboard with savanna gradient banner, progress bar, period tabs, top tags
- [x] Canvas-based Day and Week timeline rendering with gap detection
- [x] Main.kt application wiring and screen routing
- [x] Application compiles and launches successfully
- [x] Idle detection (IdleDetector + IdleViewModel + IdleDialog + Settings card)
- [x] Launch at Login (macOS Launch Agent plist management + Settings card)
- [x] Application icon (512x512 PNG, savanna clock motif, loaded in window + Dock)
- [x] Settings persistence for idle/launch via `java.util.prefs.Preferences`
- [x] Dark mode accessibility: card-specific text color tokens for contrast on cream surface
- [x] Dev feedback loop infrastructure (instance stamping, Quartz screenshots, Makefile targets, agent rule)
- [x] Memory bank documentation
- [x] Memory bank update Cursor skill (`.cursor/skills/update-memory-bank/SKILL.md`)

## In Progress
- [ ] Grant Screen Recording permission to Cursor (required for `make screenshot`)
- [ ] Fix deprecation warnings (Instant typealias, AutoMirrored icons)
- [ ] Test with real `timew` CLI data flow
- [ ] Generate `icon.icns` and `icon.ico` from `icon.png` for cross-platform packaging
- [ ] Task-timer integration (start timer with task's generated tag)

## Not Started (Phase 2+)
- [ ] Drag-to-resize intervals on timeline
- [ ] Drag-to-move intervals on timeline
- [ ] Gap visualization and quick-fill
- [ ] Undo support
- [ ] System tray icon
- [ ] Global hotkeys
- [ ] Charts (bar/pie) in Reports
- [ ] CSV/PDF export
- [ ] Notification reminders
- [ ] Unit tests for ViewModels, CLI wrapper, and TaskRepository
- [ ] Task-to-interval time aggregation (show time spent per task)

## Known Issues
- Intermittent `MetalRedrawer` crash on first launch (Skia/Metal rendering on macOS) — resolves on subsequent runs
- Deprecation warnings: `kotlinx.datetime.Instant` → `kotlin.time.Instant` (20+ occurrences in Timeline.kt, IntervalDetailPanel.kt)
- Deprecation warning: `Icons.Outlined.Label` → `Icons.AutoMirrored.Outlined.Label` (Sidebar.kt)
- `compose.material3` dependency requires `@Suppress("DEPRECATION")` in build.gradle.kts
- Missing `icon.icns` and `icon.ico` (only `icon.png` exists) — macOS DMG and Windows packaging may need these
- Requires `timew` CLI installed on system PATH to function
- Idle detection only works on macOS (uses `ioreg` command)
- `make screenshot` requires Screen Recording permission granted to Cursor (one-time macOS setup)
- `LazyColumn` cannot be nested inside `Column(Modifier.verticalScroll())` — use `weight(1f)` instead

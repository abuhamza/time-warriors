# Active Context

## Current State
Phase 1 MVP is complete with Task Management, Idle Detection, Launch at Login, and Application Icon features. The app has grown to 33 Kotlin files (~5,146 lines). The autonomous dev feedback loop infrastructure is fully built. An "Update Memory Bank" Cursor skill now exists for keeping documentation in sync.

## What Was Built
- Full project scaffolding (Gradle 8.12, Kotlin 2.1.20, Compose MP 1.10.1, Makefile)
- Domain layer: Interval model with custom JSON serialization, TimewCli wrapper for all `timew` commands
- Task management: Task model (TODO/IN_PROGRESS/DONE/ARCHIVED), TaskRepository (JSON file persistence at `~/.config/timewgui/tasks.json`), tag generation (`task:<slug>`)
- UI Theme: African savanna color palette (warm earth tones), light and dark modes, custom `TimewColors` tokens with card-specific text tokens for dark mode contrast
- ViewModels: AppState, TimerViewModel, TimelineViewModel, TagViewModel, IdleViewModel, TaskViewModel
- UI Components: Sidebar (collapsible, icon+label), TopBar (timer status with pulsing dot), Timeline (Canvas-based Day + Week), IntervalDetailPanel (slide-in editor), IntervalList, TagSelector (autocomplete), StartTimerDialog, ProgressBar, IdleDialog
- Screens: Dashboard, Timeline, Reports, Tags, Tasks, Settings (6 screens total)
- Domain services: IdleDetector (macOS ioreg-based), LaunchAtLogin (macOS plist management)
- Application icon: 512x512 PNG with savanna-themed clock motif
- Dev feedback loop: instance-stamped `make run-bg`, Quartz-based `scripts/dev-screenshot.sh`, `.cursor/rules/dev-feedback-loop.mdc`
- Memory bank update skill: `.cursor/skills/update-memory-bank/SKILL.md`

## Recently Added
1. **Task Management Feature**: Full task tracking integrated with Timewarrior tags
   - `domain/model/Task.kt` — Task data class with `TaskStatus` enum (TODO, IN_PROGRESS, DONE, ARCHIVED)
   - `domain/repository/TaskRepository.kt` — File-based JSON persistence at `~/.config/timewgui/tasks.json`, `generateTag()` creates `task:<slug>` tags for linking tasks to time intervals
   - `viewmodel/TaskViewModel.kt` — Task CRUD operations via TaskRepository
   - `ui/screens/TasksScreen.kt` — Full task management UI (413 lines) with status filtering and task cards
   - `Screen.TASKS` added to navigation enum, Tasks nav item in Sidebar with `Icons.Outlined.CheckBox`

2. **Dark Mode Accessibility Fixes**: Card-specific text color tokens
   - `textOnCardPrimary`, `textOnCardSecondary`, `textOnCardTertiary`, `borderOnCard` tokens added to `TimewColors`
   - Dark mode uses light-theme text colors on the cream card surface (#FFF5E8) for proper contrast

3. **African Savanna UI Redesign**: Canvas-drawn gradient banner, earth-tone palette overhaul, period tabs, colored bar indicators

4. **Memory Bank Update Skill**: Cursor agent skill at `.cursor/skills/update-memory-bank/SKILL.md` for automated memory bank maintenance

## Runtime Status
- Application compiles successfully (`BUILD SUCCESSFUL`, no errors)
- Dev feedback pipeline: `make run-bg` and `make wait-window` confirmed working
- `make screenshot` blocked by Screen Recording permission (one-time macOS grant for Cursor needed)
- Pre-existing deprecation warnings remain (Instant typealias, AutoMirrored icons, compose.material3)

## Known Deprecation Warnings
1. `kotlinx.datetime.Instant` typealias deprecated — should migrate to `kotlin.time.Instant` (affects IntervalDetailPanel.kt, Timeline.kt)
2. `Icons.Outlined.Label` deprecated — should use `Icons.AutoMirrored.Outlined.Label` (Sidebar.kt:74)
3. `compose.material3` dependency suppression (`@Suppress("DEPRECATION")` in build.gradle.kts)

## Next Steps
1. Grant Screen Recording permission to Cursor (System Settings → Privacy & Security → Screen Recording)
2. Fix deprecation warnings (migrate `Instant` imports, update icon references)
3. Test with actual `timew` CLI installed to validate data flow
4. Generate `icon.icns` and `icon.ico` from `icon.png` for cross-platform packaging
5. Polish UI: hover states, keyboard shortcuts, animation timing
6. Address the intermittent MetalRedrawer crash on first launch
7. Phase 2 features: drag-to-resize, drag-to-move, gap visualization, undo
8. System tray integration
9. Add unit tests for ViewModels, CLI wrapper, and TaskRepository
10. Packaging for distribution (DMG, DEB, MSI)
11. Task-timer integration: start timer with task's generated tag, link intervals to tasks

## Active Decisions
- Theme diverged from PRD spec: using warm African savanna palette instead of neutral gray/blue. Intentional design choice.
- Sidebar shows "TimeTrackAI" branding instead of "TimewGUI"
- `DashboardScreen` includes a custom Canvas-drawn savanna sunset gradient banner
- Dark mode `cardSurface` (#FFF5E8) uses dedicated `textOnCard*` tokens for contrast — resolved via session [UI accessibility fixes](fa738be6)
- Idle detection uses macOS `ioreg` fallback (no JNA dependency) — works on macOS only
- Task persistence is separate from Timewarrior — uses `~/.config/timewgui/tasks.json` (file-based, not `java.util.prefs.Preferences`)
- Task-to-interval linkage via generated tags (`task:<slug>`) — evaluated in session [To-do / task tracking evaluation](1bce6974)
- Dev feedback loop uses Gradle project property (`-PinstanceId=`) because `JAVA_TOOL_OPTIONS` env vars don't appear in process command line
- Quartz Python module installed in `.dev/venv/` (not system Python) to avoid PEP 668 restrictions on Python 3.14

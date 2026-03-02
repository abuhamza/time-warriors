# Technical Context

## Stack
- **Language**: Kotlin 2.1.20
- **UI Framework**: Compose Multiplatform 1.10.1 (Desktop)
- **Build**: Gradle 8.12 with Kotlin DSL
- **JDK**: OpenJDK 21 (bundled via jpackage for distribution)
- **Serialization**: kotlinx-serialization-json 1.8.0
- **Date/Time**: kotlinx-datetime 0.7.1
- **Coroutines**: kotlinx-coroutines-core 1.10.1, kotlinx-coroutines-swing 1.10.1
- **Icons**: compose material-icons-extended 1.7.3

## Compiler Options
- `-opt-in=kotlin.time.ExperimentalTime`
- `-opt-in=kotlinx.serialization.ExperimentalSerializationApi`

## Architecture
- CLI Integration: `TimewCli` class wraps all `timew` commands via ProcessBuilder
- Data flows through `timew export` (JSON) for reads, CLI commands for writes
- Task persistence: `TaskRepository` reads/writes `~/.config/timewgui/tasks.json`
- ViewModels use Compose `mutableStateOf` for reactive state
- No Android dependencies — pure Compose Desktop
- Tag color persistence via `java.util.prefs.Preferences`

## Project Structure
```
src/main/kotlin/com/timewgui/
├── Main.kt                          # Entry point, ViewModel wiring, screen routing, icon (166 lines)
├── domain/
│   ├── cli/TimewCli.kt              # CLI wrapper for all timew commands (132 lines)
│   ├── idle/IdleDetector.kt         # macOS idle time detection via ioreg (29 lines)
│   ├── model/
│   │   ├── Interval.kt             # Core time interval model with JSON serialization (113 lines)
│   │   ├── TagInfo.kt              # Tag with color (9 lines)
│   │   └── Task.kt                 # Task model with TaskStatus enum (20 lines)
│   ├── repository/
│   │   └── TaskRepository.kt       # File-based task persistence + tag generation (43 lines)
│   └── system/LaunchAtLogin.kt     # macOS Launch Agent plist management (51 lines)
├── viewmodel/
│   ├── AppState.kt                  # Navigation, sidebar, theme, settings (71 lines)
│   ├── IdleViewModel.kt            # Idle detection polling + dialog (92 lines)
│   ├── TimerViewModel.kt           # Active timer state + 1s ticking (113 lines)
│   ├── TimelineViewModel.kt        # Timeline data + interval CRUD (194 lines)
│   ├── TagViewModel.kt             # Tag management + color persistence (123 lines)
│   └── TaskViewModel.kt            # Task CRUD via TaskRepository (117 lines)
├── ui/
│   ├── theme/
│   │   ├── Color.kt                # African savanna palette, light + dark, card tokens (191 lines)
│   │   ├── Typography.kt           # System font + monospace (97 lines)
│   │   └── Theme.kt                # TimewGuiTheme, TimewDimensions (54 lines)
│   ├── navigation/Screen.kt        # Screen enum: DASHBOARD, TIMELINE, REPORTS, TAGS, TASKS, SETTINGS (10 lines)
│   ├── components/
│   │   ├── IdleDialog.kt           # Idle detection dialog (87 lines)
│   │   ├── Sidebar.kt              # Collapsible navigation sidebar (191 lines)
│   │   ├── TopBar.kt               # Timer status bar with pulsing dot (141 lines)
│   │   ├── Timeline.kt             # Canvas-based Day & Week timeline (465 lines)
│   │   ├── IntervalDetailPanel.kt  # Slide-in edit panel (289 lines)
│   │   ├── IntervalList.kt         # Scrollable interval list with colored bars (177 lines)
│   │   ├── TagSelector.kt          # Tag input with autocomplete (205 lines)
│   │   ├── StartTimerDialog.kt     # Modal dialog for starting timer (119 lines)
│   │   └── ProgressBar.kt          # Thin progress indicator (93 lines)
│   └── screens/
│       ├── DashboardScreen.kt      # Savanna banner, progress, intervals (288 lines)
│       ├── TimelineScreen.kt       # Day/Week timeline with detail editing (235 lines)
│       ├── ReportsScreen.kt        # Summary table with date range filtering (295 lines)
│       ├── TagsScreen.kt           # Tag browser with filtered intervals (190 lines)
│       ├── TasksScreen.kt          # Task management UI (413 lines)
│       └── SettingsScreen.kt       # Theme, idle, launch, targets (333 lines)
src/main/resources/
└── icon.png                         # 512x512 savanna-themed clock icon
```

## Build Files
- `build.gradle.kts` — Plugin config, dependencies, native distribution targets (DMG, MSI, Deb); accepts `-PinstanceId=` for dev feedback loop
- `settings.gradle.kts` — Root project name `timewgui`
- `gradle.properties` — JVM args (-Xmx2048m), Kotlin code style
- `Makefile` — Build targets (`run`, `build`, `compile`, `clean`, `package-*`) + dev feedback loop targets (`run-bg`, `stop`, `wait-window`, `screenshot`, `dev-feedback`)
- `gradlew` / `gradlew.bat` — Gradle wrapper scripts

## Dev Feedback Loop (Agent Tooling)
Autonomous UI verification for Cursor agents editing UI code:
- `scripts/dev-screenshot.sh` — Quartz-based window detection (PID or title match), screenshot via `screencapture -l`, auto-installs `pyobjc-framework-Quartz` in `.dev/venv/`
- `.cursor/rules/dev-feedback-loop.mdc` — Agent instructions triggered on `src/main/kotlin/**/*.kt` edits
- Instance stamping: `make run-bg` generates a unique ID, passes `-PinstanceId=<id>` to Gradle which forwards `-Dtimewgui.instanceId=<id>` as JVM arg; PID detected via `pgrep -f`
- State files: `.dev/app.pid`, `.dev/app.instance`, `.dev/app.log`
- Requires one-time macOS Screen Recording permission for Cursor

## Agent Tooling
- `.cursor/rules/dev-feedback-loop.mdc` — UI verification workflow (triggered on Kotlin edits)
- `.cursor/skills/update-memory-bank/SKILL.md` — Memory bank update skill (on-demand)

## Development
- Build: `make compile` or `./gradlew compileKotlin`
- Run (foreground): `make run` or `./gradlew run`
- Run (background with tracking): `make run-bg` → `make wait-window` → `make screenshot`
- Full feedback cycle: `make dev-feedback` (stop → run-bg → wait-window → screenshot)
- JAVA_HOME: `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`

## Codebase Stats
- 33 Kotlin source files
- ~5,146 total lines of code
- No test files yet
- App icon: `icon.png` (512x512) in `src/main/resources/` — still need `icon.icns` and `icon.ico` for packaging

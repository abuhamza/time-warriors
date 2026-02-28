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

## Architecture
- CLI Integration: `TimewCli` class wraps all `timew` commands via ProcessBuilder
- Data flows through `timew export` (JSON) for reads, CLI commands for writes
- ViewModels use Compose `mutableStateOf` for reactive state
- No Android dependencies — pure Compose Desktop
- Tag color persistence via `java.util.prefs.Preferences`

## Project Structure
```
src/main/kotlin/com/timewgui/
├── Main.kt                       # Application entry point, ViewModel wiring, screen routing, icon
├── domain/
│   ├── cli/TimewCli.kt           # CLI wrapper (all timew commands via ProcessBuilder)
│   ├── idle/IdleDetector.kt      # macOS idle time detection via ioreg HIDIdleTime
│   ├── model/Interval.kt         # Core data model with custom JSON serialization
│   ├── model/TagInfo.kt          # Tag with color
│   └── system/LaunchAtLogin.kt   # macOS Launch Agent plist management
├── viewmodel/
│   ├── AppState.kt               # Navigation, sidebar, theme, idle/launch settings + Preferences
│   ├── IdleViewModel.kt          # Idle detection polling, dialog state, keep/pause/discard actions
│   ├── TimerViewModel.kt         # Active timer state + ticking (1s updates)
│   ├── TimelineViewModel.kt      # Timeline data + interval CRUD operations
│   └── TagViewModel.kt           # Tag management + color persistence
├── ui/
│   ├── theme/
│   │   ├── Color.kt              # African savanna color palette (light + dark)
│   │   ├── Typography.kt         # System font + monospace typography
│   │   └── Theme.kt              # TimewGuiTheme composable, TimewDimensions
│   ├── navigation/Screen.kt      # Screen enum (DASHBOARD, TIMELINE, REPORTS, TAGS, SETTINGS)
│   ├── components/
│   │   ├── IdleDialog.kt         # Idle detection dialog with Keep/Pause/Stop buttons
│   │   ├── Sidebar.kt            # Collapsible navigation sidebar with icons
│   │   ├── TopBar.kt             # Timer status bar with pulsing dot
│   │   ├── Timeline.kt           # Canvas-based Day & Week timeline (~387 lines)
│   │   ├── IntervalDetailPanel.kt # Slide-in edit panel for intervals
│   │   ├── IntervalList.kt       # Scrollable interval list with colored bars
│   │   ├── TagSelector.kt        # Tag input with autocomplete dropdown
│   │   ├── StartTimerDialog.kt   # Modal dialog for starting timer with tags
│   │   └── ProgressBar.kt        # Thin progress indicator with hover percentage
│   └── screens/
│       ├── DashboardScreen.kt    # Home screen with savanna banner, progress, intervals
│       ├── TimelineScreen.kt     # Day/Week timeline with interval detail editing
│       ├── ReportsScreen.kt      # Summary table with date range and tag filtering
│       ├── TagsScreen.kt         # Tag browser with filtered interval list
│       └── SettingsScreen.kt     # Theme, idle detection, launch at login, targets
src/main/resources/
└── icon.png                      # 512x512 savanna-themed clock icon
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

## Development
- Build: `make compile` or `./gradlew compileKotlin`
- Run (foreground): `make run` or `./gradlew run`
- Run (background with tracking): `make run-bg` → `make wait-window` → `make screenshot`
- Full feedback cycle: `make dev-feedback` (stop → run-bg → wait-window → screenshot)
- JAVA_HOME: `/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home`
  (also available at `/opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home`)

## Codebase Stats
- 25 Kotlin source files
- ~4,200 total lines of code
- No test files yet
- App icon: `icon.png` (512x512) in `src/main/resources/` — still need `icon.icns` and `icon.ico` for packaging

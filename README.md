# TimewGUI

A modern desktop GUI for [Timewarrior](https://timewarrior.net/), built with Compose Multiplatform. Track your time visually while keeping full CLI data compatibility.

## Features

- **Timer** — Start, stop, continue, and cancel intervals with tags and annotations
- **Timeline** — Day and week views with canvas-based hourly visualization and color-coded blocks
- **Reports** — Filter by today/week/month/year with time-by-tag bar charts
- **Tags** — Color-coded earth-tone palette, archive/unarchive, per-tag interval browsing
- **Tasks** — Local task board (TODO/In Progress/Done/Archived) with timer integration and time aggregation
- **Overtime Tracking** — Daily/cumulative balance vs. configurable targets, workday filtering, excluded date ranges
- **Idle Detection** — macOS HID idle polling with configurable threshold and pause/discard prompts
- **Launch at Login** — macOS Launch Agent integration
- **absence.io Integration** — Sync approved absences to auto-exclude from overtime calculations
- **Dark/Light Theme** — "African Savanna" warm earth-tone palette with system theme detection
- **Dashboard** — Progress indicators, overtime cards, top tags, and a canvas-drawn savanna banner
- **Interval Editing** — Modify start/end times, move, split, join, retag, and annotate intervals
- **Settings** — Daily/weekly hour targets, default context tags, idle threshold, and more
- **Full CLI Compatibility** — All data flows through `timew` commands; no direct file access

## Requirements

- [Timewarrior](https://timewarrior.net/) (`timew`) installed and on PATH
- JDK 21 (e.g., `brew install openjdk@21` on macOS, or `apt install openjdk-21-jdk` on Debian/Ubuntu)

## Installation

### Download

Grab the latest `.dmg`, `.deb`, or `.msi` from the [Releases](https://github.com/abuhamza/time-warriors/releases) page.

### Debian/Ubuntu (apt)

```bash
# Add the signing key
curl -fsSL https://abuhamza.github.io/time-warriors/public.key \
  | sudo gpg --dearmor -o /usr/share/keyrings/timewgui.gpg

# Add the repository
echo "deb [signed-by=/usr/share/keyrings/timewgui.gpg arch=amd64] https://abuhamza.github.io/time-warriors stable main" \
  | sudo tee /etc/apt/sources.list.d/timewgui.list

# Install
sudo apt update && sudo apt install timewgui
```

### macOS (Homebrew)

```bash
brew tap abuhamza/time-warriors https://github.com/abuhamza/time-warriors
brew install --cask timewgui
```

### Build from Source

```bash
git clone https://github.com/abuhamza/time-warriors.git && cd time-warriors
make run              # Run directly
make build            # Full build
make package-dmg      # Package macOS .dmg
make package-deb      # Package Linux .deb
make package-msi      # Package Windows .msi
```

## Architecture

TimewGUI follows **MVVM with CLI-as-backend** — the GUI never reads or writes Timewarrior's `.data` files directly. All data flows through `timew` CLI commands via a single `TimewCli` wrapper.

```
AppState (owns TimewCli singleton, navigation, settings)
├── TimerViewModel     — active timer state, 1-second tick
├── TimelineViewModel  — interval CRUD, day/week navigation
├── TagViewModel       — tag colors, archive state
├── TaskViewModel      — file-based task persistence
├── IdleViewModel      — macOS idle detection polling
└── OvertimeViewModel  — balance calculation, absence.io sync
```

### Source Layout

| Directory | Purpose |
|-----------|---------|
| `domain/cli/` | `TimewCli` — sole interface to Timewarrior |
| `domain/model/` | Data classes: `Interval`, `Task`, `TagInfo`, `ExcludedDateRange`, `RecurrenceRule` |
| `domain/api/` | absence.io client and Hawk authentication |
| `domain/repository/` | File-based task persistence |
| `domain/idle/` | macOS HID idle detection |
| `domain/system/` | Launch at Login (macOS Launch Agent) |
| `domain/` | `RecurrenceEngine` — recurring excluded-date expansion |
| `viewmodel/` | `AppState` + all ViewModels |
| `ui/theme/` | African Savanna colors, typography, dimensions |
| `ui/components/` | Reusable composables (Sidebar, TopBar, Timeline, etc.) |
| `ui/screens/` | Dashboard, Timeline, Reports, Tags, Tasks, Settings |
| `ui/navigation/` | Screen enum for routing |

## Tech Stack

- **Kotlin 2.1.20** / **Compose Multiplatform 1.10.1** (Desktop)
- **JDK 21**
- **kotlinx.serialization** for JSON
- **kotlinx.coroutines** for async CLI calls
- **kotlinx.datetime** for date/time handling
- **java.util.prefs.Preferences** for settings persistence

## Configuration

Settings are persisted via `java.util.prefs.Preferences` and include:

- Theme (System / Light / Dark)
- Idle detection toggle and threshold (1–120 minutes)
- Launch at Login (macOS)
- Daily and weekly hour targets
- Overtime tracking start date and workday selection
- Default context tags for new tasks
- absence.io API credentials

Tasks are stored in `~/.config/timewgui/tasks.json`.

## Development

```bash
make run              # Run in foreground
make compile          # Compile (~12s incremental)
make test             # Run all tests
make dev-feedback     # Full cycle: stop -> run-bg -> wait -> screenshot
make clean            # Clean build artifacts
make help             # Show all targets
```

### CI/CD

GitHub Actions runs on every push and PR against `main`:

- **Build & test** on macOS, Ubuntu, and Windows
- **Package** `.dmg`, `.deb`, and `.msi` on tagged releases (`v*`)
- **Publish** a GitHub Release with all three packages
- **Update** the Homebrew Cask and Debian APT repository automatically

## Contributing

Contributions are welcome! Here's how to get started:

### Getting Set Up

1. Fork the repository and clone your fork
2. Install [Timewarrior](https://timewarrior.net/) (`timew`) and JDK 21
3. Run `make run` to verify the app starts
4. Run `make test` to verify tests pass

### Making Changes

1. Create a feature branch from `main`
2. Follow existing patterns:
   - **MVVM** — UI logic lives in ViewModels, not composables
   - **CLI-as-backend** — all Timewarrior interaction goes through `TimewCli.kt`
   - **African Savanna theme** — use `TimewTheme.colors` for custom color tokens, not `MaterialTheme.colorScheme`
3. All `TimewCli` methods return `Result<T>` — handle both success and failure
4. After any data mutation, call `refreshIntervals()` on `TimelineViewModel`
5. Add tests for new logic in `src/test/kotlin/`

### Submitting

1. Run `make test` to verify all tests pass
2. Run `make compile` to check for warnings
3. Submit a pull request against `main` with a clear description of what and why

### Code Style

- Kotlin official code style (`kotlin.code.style=official`)
- Keep composables focused — extract reusable components to `ui/components/`
- Use `Dispatchers.IO` for CLI calls, `Dispatchers.Main.immediate` for UI state
- Prefer `Canvas`-based rendering for timeline visualizations

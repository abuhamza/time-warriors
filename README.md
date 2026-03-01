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
- JDK 21 (e.g., `brew install openjdk@21`)

## Installation

### Download

Grab the latest `.dmg` from the [Releases](../../releases) page.

### Build from Source

```bash
git clone <repo-url> && cd time-warriors
make run        # Run directly
make build      # Full build
make package-dmg  # Package macOS .dmg
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
| `domain/model/` | Data classes: `Interval`, `Task`, `TagInfo`, `ExcludedDateRange` |
| `domain/api/` | absence.io Hawk authentication |
| `domain/repository/` | File-based task persistence |
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
make dev-feedback     # Full cycle: stop → run-bg → wait → screenshot
make clean            # Clean build artifacts
make help             # Show all targets
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes (follow existing patterns — MVVM, CLI-as-backend, African Savanna theme)
4. Run `make test` to verify all tests pass
5. Submit a pull request

## License

See [LICENSE](LICENSE) for details.

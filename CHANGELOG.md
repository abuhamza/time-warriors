# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-03-01

### Added

- **Timer management** — start, stop, continue, and cancel time tracking intervals with real-time elapsed display
- **Tag system** — assign tags to intervals with 8 earth-tone color slots, archive/unarchive support, and per-tag interval browsing
- **Annotations** — add notes to intervals for context
- **Timeline views** — day and week modes with canvas-based hourly visualization, color-coded tag blocks, and daily totals
- **Date navigation** — previous/next day/week, jump to specific date, jump to today
- **Interval editing** — modify start/end times, move, split, join, retag, and annotate intervals via detail panel
- **Dashboard** — daily progress indicator, overtime balance card, top weekly tags, and canvas-drawn African Savanna banner
- **Reports screen** — today/week/month/year filters with interval table and time-by-tag bar chart
- **Tags screen** — two-pane layout with active/archived filter and tag-scoped interval browser
- **Tasks screen** — local task board with TODO/In Progress/Done/Archived statuses, timer integration, and time aggregation per task
- **Task persistence** — file-based storage at `~/.config/timewgui/tasks.json`
- **Settings screen** — configurable daily/weekly hour targets, theme selection, default context tags
- **Overtime tracking** — daily and cumulative balance calculation against configurable targets with workday filtering
- **Excluded date ranges** — manually exclude vacation/holiday periods from overtime calculations
- **absence.io integration** — sync approved absences via Hawk-authenticated API to auto-exclude from overtime
- **Idle detection** — macOS HID idle time polling with configurable threshold and pause/discard dialog
- **Launch at Login** — macOS Launch Agent creation for automatic startup
- **Dark/Light theme** — "African Savanna" warm earth-tone palette with system theme detection
- **Collapsible sidebar** — navigation with "TimeTrackAI" branding and expand/collapse toggle
- **Top bar timer** — persistent active timer display with quick start/stop controls
- **CLI-as-backend architecture** — all data flows through `timew` commands for full Timewarrior compatibility
- **Test suite** — 41 tests across 8 test files covering models, CLI wrapper, repository, and ViewModels

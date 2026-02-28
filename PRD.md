# Product Requirements Document: Timewarrior GUI

## 1. Overview

### 1.1 Product Name
**TimewGUI** — A modern desktop GUI for Timewarrior (`timew`)

### 1.2 Vision
Provide a beautiful, intuitive graphical interface on top of the Timewarrior CLI, enabling users to visually track, manage, and analyze their time without memorizing CLI syntax — while preserving full CLI data compatibility.

### 1.3 Problem Statement
Timewarrior is a powerful command-line time tracker, but it has no native GUI. Users must remember command syntax, interval formats, hint shortcuts, and tag conventions. Reviewing time data requires reading terminal tables. There is no visual timeline, no drag-to-edit, and no dashboard — all common expectations from modern time-tracking tools.

Existing workarounds (custom scripts, Waybar widgets, ActivityWatch integration) are fragmented and require significant setup effort.

### 1.4 Target Users
- Developers and power users already using Timewarrior CLI who want visual insight
- Teams wanting a lower barrier to entry for Timewarrior adoption
- Anyone who prefers graphical interaction for time tracking but values Timewarrior's data model

### 1.5 Non-Goals
- Replacing the CLI — the GUI is a complement, not a replacement
- Building a SaaS or cloud-hosted service — this is a local desktop application
- Reimplementing Timewarrior's storage engine — data is read/written via `timew` commands and JSON export

---

## 2. Timewarrior CLI Reference (Baseline)

The GUI must support all core Timewarrior concepts and commands. Below is the exhaustive command inventory from `timew` v1.9.x.

### 2.1 Data Model

| Concept        | Description |
|----------------|-------------|
| **Interval**   | A block of tracked time with a start, optional end, tags, and annotation. Open interval = currently tracking. |
| **Tag**        | One or more words associated with an interval. Tags are the primary categorization mechanism. |
| **Annotation** | Free-text note attached to an interval. |
| **ID**         | `@<n>` — a dynamic, session-relative identifier for intervals (most recent = `@1`). |
| **Range/Hint** | Date range shortcuts like `:week`, `:month`, `:yesterday`, `:lastweek`, etc. |

### 2.2 Commands by Category

#### Tracking

| Command | Syntax | Description |
|---------|--------|-------------|
| `start` | `timew start [<date>] [<tag> ...]` | Start tracking time, optionally with tags and a custom start time |
| `stop` | `timew stop [<tag> ...]` | Stop tracking time. If tags given, only stop those tags |
| `cancel` | `timew cancel` | Cancel active tracking without saving |
| `continue` | `timew continue [@<id>] [<date>\|<interval>]` | Resume a previous interval's tags |
| `track` | `timew track <interval> [<tag> ...]` | Retroactively add a closed interval |

#### Modifying Intervals

| Command | Syntax | Description |
|---------|--------|-------------|
| `delete` | `timew delete @<id> [@<id> ...]` | Remove intervals |
| `move` | `timew move @<id> <date>` | Shift interval start time (preserves duration) |
| `modify` | `timew modify (start\|end) @<id> <date>` | Change start or end of an interval |
| `modify` | `timew modify range @<id> <interval>` | Replace entire interval range |
| `lengthen` | `timew lengthen @<id> [@<id> ...] <duration>` | Extend interval(s) by a duration |
| `shorten` | `timew shorten @<id> [@<id> ...] <duration>` | Reduce interval(s) by a duration |
| `resize` | `timew resize @<id> <duration>` | Set interval to exact duration |
| `split` | `timew split @<id> [@<id> ...]` | Split interval into two equal halves |
| `join` | `timew join @<id> @<id>` | Merge two intervals into one |

#### Tag & Annotation Management

| Command | Syntax | Description |
|---------|--------|-------------|
| `tag` | `timew tag @<id> [@<id> ...] <tag> [<tag> ...]` | Add tags to existing intervals |
| `untag` | `timew untag @<id> [@<id> ...] <tag> [<tag> ...]` | Remove tags from intervals |
| `retag` | `timew retag @<id> [@<id> ...] <tag> [<tag> ...]` | Replace all tags on intervals |
| `tags` | `timew tags [<interval>] [<tag> ...]` | List all known tags |
| `annotate` | `timew annotate @<id> [@<id> ...] <annotation>` | Add/replace annotation text |

#### Reporting & Export

| Command | Syntax | Description |
|---------|--------|-------------|
| `summary` | `timew summary [<interval>] [<tag> ...]` | Tabular summary with totals |
| `day` | `timew day [<interval>] [<tag> ...]` | Visual day chart |
| `week` | `timew week [<interval>] [<tag> ...]` | Visual week chart |
| `month` | `timew month [<interval>] [<tag> ...]` | Visual month chart |
| `export` | `timew export [<id>... \| <range> <tag>...]` | JSON export of intervals |
| `report` | `timew [report] <report> [<interval>] [<tag> ...]` | Run extension reports |
| `gaps` | `timew gaps [<interval>] [<tag> ...]` | Show untracked gaps |

#### Configuration & System

| Command | Syntax | Description |
|---------|--------|-------------|
| `config` | `timew config [<name> [<value>]]` | Get/set configuration values |
| `show` | `timew show` | Display full configuration |
| `get` | `timew get <DOM> [<DOM> ...]` | Query DOM values |
| `extensions` | `timew extensions` | List installed extensions |
| `diagnostics` | `timew diagnostics` | System diagnostics |
| `help` | `timew help [<topic>]` | Display help |
| `undo` | `timew undo` | Undo last command |

### 2.3 Range Hints

| Hint | Meaning |
|------|---------|
| `:all` | All tracked time |
| `:today` / `:day` | Current day (24h) |
| `:yesterday` | Previous day |
| `:week` | Current week |
| `:lastweek` | Previous week |
| `:fortnight` | This week and last |
| `:month` | Current month |
| `:lastmonth` | Previous month |
| `:quarter` | Current quarter |
| `:lastquarter` | Previous quarter |
| `:year` | Current year |
| `:lastyear` | Previous year |
| `:monday` .. `:sunday` | Since previous named day |

### 2.4 Export JSON Schema

```json
[
  {
    "id": 1,
    "start": "20260227T074517Z",
    "end": "20260227T150117Z",
    "tags": ["working-hours"],
    "annotation": "optional note"
  }
]
```

- Dates are ISO 8601 compact format (`YYYYMMDDTHHmmssZ`)
- `end` is absent for open (active) intervals
- `tags` is an array of strings
- `annotation` is optional

### 2.5 Data Storage

| Item | Path |
|------|------|
| Config | `$XDG_CONFIG_HOME/timewarrior/timewarrior.cfg` (or `~/.timewarrior/timewarrior.cfg`) |
| Data | `$XDG_DATA_HOME/timewarrior/data/YYYY-MM.data` (or `~/.timewarrior/data/`) |

---

## 3. Functional Requirements

### 3.1 Timer Controls (P0 — Must Have)

| Feature | Description | Mapped CLI Commands |
|---------|-------------|---------------------|
| **Start Timer** | One-click start with tag selector/autocomplete. Optional backdated start time. | `timew start [<date>] [<tag>...]` |
| **Stop Timer** | Stop the active timer. Show elapsed time before confirming. | `timew stop` |
| **Cancel Timer** | Discard the active tracking without saving. | `timew cancel` |
| **Continue** | Resume tracking with the tags from any previous interval (pick from list). | `timew continue [@<id>]` |
| **Active Indicator** | Always-visible indicator showing if a timer is running, its tags, and elapsed time. Ideally a system tray icon or menubar widget. | `timew` (default command) |
| **Quick Track** | Retroactively log a past interval with start/end times and tags. | `timew track <interval> [<tag>...]` |

### 3.2 Timeline View (P0 — Must Have)

| Feature | Description |
|---------|-------------|
| **Day Timeline** | Horizontal or vertical timeline showing all intervals for a given day. Color-coded by tag. Gaps visually indicated. |
| **Week Timeline** | 7-day view with each day as a column/row. Daily totals shown. |
| **Navigation** | Previous/Next day/week controls. Jump-to-date picker. |
| **Zoom** | Zoom in/out on the timeline granularity (15min, 30min, 1h cells). |
| **Active Timer** | The currently running interval grows in real-time on the timeline. |

### 3.3 Interval Editing (P0 — Must Have)

| Feature | Description | Mapped CLI Commands |
|---------|-------------|---------------------|
| **Drag to Resize** | Drag interval edges on timeline to change start/end. | `timew modify start/end @<id> <date>` |
| **Drag to Move** | Drag interval body on timeline to reposition (preserving duration). | `timew move @<id> <date>` |
| **Click to Edit** | Click an interval to open detail panel: edit tags, annotation, times. | `timew modify`, `timew retag`, `timew annotate` |
| **Delete** | Delete interval from context menu or detail panel. Confirmation required. | `timew delete @<id>` |
| **Split** | Split an interval into two halves from context menu. | `timew split @<id>` |
| **Join** | Select two adjacent intervals and merge them. | `timew join @<id> @<id>` |
| **Lengthen / Shorten** | Quick-action buttons or keyboard shortcuts to adjust by preset durations (5m, 15m, 30m, 1h). | `timew lengthen/shorten @<id> <dur>` |
| **Undo** | Undo the last operation. | `timew undo` |

### 3.4 Tag Management (P1 — Should Have)

| Feature | Description | Mapped CLI Commands |
|---------|-------------|---------------------|
| **Tag Autocomplete** | When starting or editing, suggest from existing tags. | `timew tags` |
| **Tag Colors** | Assign persistent colors to tags for visual differentiation. | GUI-only config |
| **Tag Browser** | View all tags, filter intervals by tag, see per-tag totals. | `timew tags`, `timew summary <tag>` |
| **Bulk Retag** | Select multiple intervals and change their tags at once. | `timew retag @<id>... <tag>...` |
| **Tag Hierarchy** (optional) | Support parent/child tag notation (e.g., `work/meetings`). | Convention on top of flat tags |

### 3.5 Reporting & Analytics (P1 — Should Have)

| Feature | Description | Mapped CLI Commands |
|---------|-------------|---------------------|
| **Summary Table** | Tabular view with date, tags, duration, daily/weekly totals. Filterable by tag and range. | `timew summary [<interval>] [<tag>...]` |
| **Charts** | Bar charts (daily hours), pie charts (tag distribution), trend lines (weekly comparison). | Computed from `timew export` data |
| **Gap Analysis** | Highlight untracked gaps in the timeline. Click gap to fill with new interval. | `timew gaps` |
| **Date Range Picker** | Select arbitrary date ranges. Preset buttons for `:today`, `:week`, `:month`, `:quarter`, `:year` and their `:last*` variants. | Range hints |
| **Export** | Export filtered data as JSON, CSV, or PDF report. | `timew export [<range>] [<tag>...]` |

### 3.6 Dashboard (P1 — Should Have)

| Feature | Description |
|---------|-------------|
| **Today Widget** | Hours tracked today vs target, with tag breakdown. |
| **Week Widget** | Weekly progress toward a configurable weekly target. |
| **Streak / Consistency** | Consecutive days with logged time. |
| **Top Tags** | Most-used tags in the selected period. |
| **Recent Intervals** | List of recent entries with quick-continue buttons. |

### 3.7 Configuration (P2 — Nice to Have)

| Feature | Description | Mapped CLI Commands |
|---------|-------------|---------------------|
| **Settings Panel** | GUI for editing `timewarrior.cfg` values: confirmation, report defaults, color theme. | `timew config`, `timew show` |
| **Daily/Weekly Targets** | Set expected work hours per day/week for progress tracking. | GUI-only config |
| **Working Hours** | Define working hour boundaries for the timeline view. | GUI-only config |
| **Idle Detection** | Detect system idle time and prompt to fill or discard gaps. | GUI-only feature |
| **Notifications** | Remind after N hours to stop/switch tasks. | GUI-only feature |

### 3.8 System Integration (P2 — Nice to Have)

| Feature | Description |
|---------|-------------|
| **System Tray / Menu Bar** | Persistent icon showing active timer status. Click to start/stop/switch. |
| **Global Hotkeys** | System-wide keyboard shortcuts to start/stop without focusing the app. |
| **Auto-Start** | Optionally launch at login. |
| **Dark/Light Mode** | Follow system theme or user preference. |

---

## 4. Non-Functional Requirements

### 4.1 Architecture

| Aspect | Decision |
|--------|----------|
| **CLI Integration** | All mutations go through `timew` CLI commands. The GUI never directly writes to `.data` files. |
| **Data Reading** | Use `timew export` for reading intervals. `timew tags` for tag list. `timew show` for config. |
| **Refresh Strategy** | Poll `timew export` on focus/interval or use filesystem watcher on data directory. |
| **State** | GUI-specific state (tag colors, targets, window size) stored in a separate config file, not in `timewarrior.cfg`. |

### 4.2 Technology Stack

**Kotlin Multiplatform + Compose Multiplatform**

| Aspect | Detail |
|--------|--------|
| **Language** | Kotlin |
| **UI Framework** | Compose Multiplatform (JetBrains) |
| **Platforms** | macOS (Apple Silicon + Intel), Linux, Windows — single codebase |
| **Async** | Kotlin Coroutines for non-blocking CLI calls |
| **Build** | Gradle with Kotlin DSL |
| **Packaging** | Native installers via `conveyor`, `jpackage`, or Compose Desktop packaging plugin |
| **Min JDK** | JDK 17+ (bundled with the app, no user-installed JRE required) |

**Why Compose Multiplatform:**
- True native desktop application — no webview, no Electron overhead
- Single codebase for macOS, Linux, and Windows
- Material Design 3 components provide a clean, modern foundation without manual styling
- Canvas API and gesture handling are well-suited for custom timeline rendering and drag interactions
- Kotlin coroutines make shelling out to `timew` commands clean and non-blocking
- System tray, menu bar, and global hotkey APIs available via native interop
- JetBrains actively maintains the framework with frequent releases

### 4.3 Performance

- App launch: < 1 second
- Timer start/stop: < 200ms perceived latency
- Timeline rendering with 1000+ intervals: < 500ms
- Export/chart generation: < 2 seconds for 1 year of data

### 4.4 Platform Support

- macOS (primary — Apple Silicon + Intel)
- Linux (X11 + Wayland)
- Windows (secondary)

### 4.5 Accessibility

- Full keyboard navigation
- Screen reader support for all interactive elements
- Sufficient color contrast ratios (WCAG AA)
- Resizable UI that works from 1024x768 to 4K

### 4.6 Design Language

#### Philosophy

Clean, quiet, information-dense. Color is used only for meaning (tag differentiation, active state, warnings), never for decoration. No gradients, no drop shadows on cards, no glassmorphism, no rounded-everything. The interface should feel like a precision tool, not a marketing page.

**Reference products:** Linear, Raycast, Things 3, Figma's UI, GitHub's new interface.

#### Color System

| Token | Light Mode | Dark Mode | Usage |
|-------|-----------|-----------|-------|
| `bg-primary` | `#FFFFFF` | `#141414` | Main background |
| `bg-secondary` | `#F7F7F8` | `#1C1C1E` | Sidebar, panels, cards |
| `bg-tertiary` | `#EBEBEF` | `#2C2C2E` | Hover states, timeline gaps |
| `border` | `#E2E2E6` | `#3A3A3C` | Dividers, input borders |
| `text-primary` | `#1A1A1A` | `#F0F0F0` | Headings, primary content |
| `text-secondary` | `#6B6B76` | `#8E8E93` | Labels, metadata, timestamps |
| `text-tertiary` | `#9B9BA5` | `#636366` | Placeholders, disabled text |
| `accent` | `#2563EB` | `#3B82F6` | Active timer indicator, primary actions, focused state |
| `success` | `#16A34A` | `#22C55E` | Timer running, tracking active |
| `destructive` | `#DC2626` | `#EF4444` | Delete actions, cancel |
| `warning` | `#D97706` | `#F59E0B` | Gaps, missing data |

Tag colors use a muted, desaturated palette — never neon or fully saturated:

| Tag Color Slot | Light | Dark |
|----------------|-------|------|
| Slot 1 | `#6366F1` (indigo) | `#818CF8` |
| Slot 2 | `#0891B2` (cyan) | `#22D3EE` |
| Slot 3 | `#059669` (emerald) | `#34D399` |
| Slot 4 | `#D97706` (amber) | `#FBBF24` |
| Slot 5 | `#DC2626` (red) | `#F87171` |
| Slot 6 | `#7C3AED` (violet) | `#A78BFA` |
| Slot 7 | `#DB2777` (pink) | `#F472B6` |
| Slot 8 | `#65A30D` (lime) | `#A3E635` |

Tags are auto-assigned a color from this palette on first use. Users can override.

#### Typography

| Element | Font | Weight | Size |
|---------|------|--------|------|
| App title / Section headers | System sans-serif (SF Pro, Segoe UI, Inter) | Semibold (600) | 16px |
| Subsection headers | System sans-serif | Medium (500) | 14px |
| Body text / Table cells | System sans-serif | Regular (400) | 13px |
| Labels / Metadata | System sans-serif | Regular (400) | 12px |
| Monospace (times, durations) | System monospace (SF Mono, Cascadia Mono, JetBrains Mono) | Regular (400) | 13px |

Use the platform's native system font stack. No custom web fonts. Times and durations are always rendered in monospace for alignment.

#### Spacing & Layout

| Rule | Value |
|------|-------|
| Base unit | 4px |
| Component padding | 8px / 12px |
| Section gap | 16px |
| Sidebar width | 200px (collapsible to icon-only 48px) |
| Border radius | 4px for inputs and buttons, 6px for cards/panels, 0px for timeline blocks |
| Timeline interval blocks | Sharp corners (0px radius) — intervals are precise data, not decorative pills |

#### Component Style

**Buttons:**
- Primary: Solid fill with `accent` color, white text. No shadow, no gradient.
- Secondary: Transparent background, `text-secondary` color, subtle border on hover.
- Destructive: `destructive` color, only appears with confirmation step.
- All buttons: 32px height, 4px radius, no elevation.

**Inputs:**
- 1px solid `border` color. No inner shadow. Focus ring: 2px `accent` with 1px offset.
- Time inputs use monospace font with explicit format hint (`HH:mm`).

**Timeline Blocks:**
- Solid fill with tag color at ~85% opacity (light mode) or ~40% opacity (dark mode).
- No border, no shadow, no rounded corners.
- Active/running interval has a subtle 2px left accent border in `success` color and a pulsing dot.
- Gaps are rendered as the `bg-tertiary` color with a dashed top/bottom border.

**Sidebar:**
- Flat list of navigation items. No icons-only unless collapsed.
- Active item: `bg-tertiary` background, `text-primary` color, 2px left accent border.
- Hover: `bg-tertiary` background.

**Cards / Panels:**
- `bg-secondary` background. 1px `border` divider. No shadow, no elevation.
- Section headers inside panels are `text-secondary` uppercase 11px tracking-wide labels.

**Tables:**
- No zebra striping. Rows separated by 1px `border` lines.
- Header row: `text-secondary`, uppercase, 11px, medium weight.
- Hover row: `bg-tertiary` background.

#### Motion & Animation

| Interaction | Animation |
|-------------|-----------|
| Panel open/close | 150ms ease-out slide |
| Hover transitions | 100ms ease color/background |
| Timer tick | Update elapsed time text every second, no animation |
| Active timer pulse | Subtle opacity pulse (0.7 to 1.0) on the status dot, 2s cycle |
| Timeline zoom | 200ms ease scale transition |
| Drag interactions | No animation — immediate, direct manipulation |

No bouncy spring animations. No page transitions. No loading spinners for operations under 200ms. For longer operations, use a thin indeterminate progress bar at the top of the content area (Linear-style).

#### Iconography

- Use a minimal, monoline icon set (Lucide or Phosphor, regular weight).
- 16px default size, 20px for primary actions.
- Icons are `text-secondary` color by default, `text-primary` on hover/active.
- No filled icons except for the active timer status indicator.

#### Dark / Light Mode

- Follow system preference by default. User can override in settings.
- Both modes must be first-class — not an afterthought dark theme bolted on.
- Charts and visualizations must be legible in both modes.

---

## 5. User Flows

### 5.1 Start Tracking

```
User opens app
  → Sees dashboard with "Start Timer" button
  → Clicks "Start Timer"
  → Tag selector appears (autocomplete from existing tags)
  → User types/selects tags (e.g., "work", "meetings")
  → Clicks "Start" or presses Enter
  → GUI executes: timew start work meetings
  → Timer indicator shows elapsed time
  → Timeline updates with growing interval
```

### 5.2 Edit a Past Interval

```
User navigates to Day view
  → Sees timeline with colored intervals
  → Clicks on an interval block
  → Detail panel slides in from the right:
      - Start time (editable)
      - End time (editable)
      - Tags (editable with autocomplete)
      - Annotation (text field)
      - Duration (read-only, computed)
      - Actions: [Split] [Delete] [Duplicate]
  → User changes the end time
  → Clicks "Save"
  → GUI executes: timew modify end @<id> <new-time>
  → Timeline re-renders
```

### 5.3 Review Weekly Summary

```
User clicks "Reports" in sidebar
  → Default view: current week summary
  → Table shows: Date | Tags | Duration | Daily Total
  → Bar chart shows hours per day
  → Pie chart shows tag distribution
  → User clicks ":lastweek" preset
  → Data refreshes for previous week
  → User clicks "Export CSV"
  → File save dialog opens
```

### 5.4 Fill a Gap

```
User opens Day timeline
  → Sees a gray gap between two intervals
  → Clicks the gap
  → "Quick Track" dialog appears:
      - Start: pre-filled from previous interval's end
      - End: pre-filled from next interval's start
      - Tags: empty with autocomplete
  → User adds tags and clicks "Save"
  → GUI executes: timew track <start> - <end> <tags>
  → Gap disappears, new interval appears
```

---

## 6. UI Wireframe Description

### 6.1 Main Layout — Timeline View (Day)

```
┌──────────────────────────────────────────────────────────────────┐
│  TimewGUI                               ● 2h 14m  [Stop]  [⌘K] │
├────────┬─────────────────────────────────────────────────────────┤
│        │  ◀  Thu 27 Feb 2026  ▶                    7h 16m / 8h  │
│  HOME  │─────────────────────────────────────────────────────────│
│        │  07   08   09   10   11   12   13   14   15   16   17  │
│  TIME  │  ·····█████████████████████████░░░░████████████████···  │
│  LINE  │       working-hours            gap  working-hours       │
│        │─────────────────────────────────────────────────────────│
│  REPO  │  INTERVALS                                              │
│  RTS   │                                                         │
│        │  08:45 — 12:30   working-hours              3h 45m     │
│  TAGS  │  13:00 — 16:01   working-hours              3h 01m  ●  │
│        │                                                         │
│  ────  │─────────────────────────────────────────────────────────│
│  SETT  │  TODAY  7h 16m        THIS WEEK  31h 40m               │
│  INGS  │  ───── target 8h     ────────── target 40h             │
│        │  ██████████░░  91%   █████████░░░░░░░  79%             │
└────────┴─────────────────────────────────────────────────────────┘
```

- Sidebar is text-only navigation, uppercase 11px labels, no icons.
- Top bar shows the running timer with a green status dot and monospace elapsed time.
- `[⌘K]` opens a command palette for keyboard-driven users.
- Timeline uses sharp-cornered blocks. The active interval has a pulsing green dot.
- Below the timeline: a flat interval list with monospace times and right-aligned durations.
- Bottom section: progress bars are thin (4px height), muted colors, no percentage labels unless hovered.

### 6.2 Interval Detail Panel (Slide-in from right)

```
                                          ┌─────────────────────┐
                                          │  EDIT INTERVAL      │
                                          │                     │
                                          │  Start              │
                                          │  [08:45        ]    │
                                          │                     │
                                          │  End                │
                                          │  [12:30        ]    │
                                          │                     │
                                          │  Duration           │
                                          │  3h 45m             │
                                          │                     │
                                          │  Tags               │
                                          │  [working-hours ×]  │
                                          │  [+ add tag     ]   │
                                          │                     │
                                          │  Annotation         │
                                          │  [               ]  │
                                          │                     │
                                          │  ─────────────────  │
                                          │  Split   Delete     │
                                          │              [Save] │
                                          └─────────────────────┘
```

- Panel width: 280px fixed. Slides in with 150ms ease-out.
- Section labels are uppercase 11px `text-secondary`.
- Time inputs use monospace font. Duration is computed, non-editable, dimmed.
- Tags are chips with `×` dismiss. `+ add tag` opens inline autocomplete.
- Destructive actions (Delete) are text-only in `text-secondary`, turn `destructive` on hover.
- Save is the only solid-fill accent button.

### 6.3 Week Timeline View

```
├────────┬──────────────────────────────────────────────────────┤
│        │  ◀  Week 9 · Feb 23 — Mar 1, 2026  ▶    31h 40m    │
│        │──────────────────────────────────────────────────────│
│        │        07  08  09  10  11  12  13  14  15  16  17   │
│        │  Mon   ··  ██  ██  ██  ░░  ░░  ░░  ░░  ░░  ░░  ··  │  1h 36m
│        │  Tue   ··  ██  ██  ██  ██  ██  ██  ░░  ██  ██  ██  │  7h 24m
│        │  Wed   ··  ██  ██  ██  ██  ██  ██  ░░  ██  ██  ██  │  7h 43m
│        │  Thu   ██  ██  ██  ██  ██  ██  ██  ██  ██  ░░  ░░  │  7h 40m
│        │  Fri   ··  ██  ██  ██  ██  ██  ██  ██  ██  ██  ··  │  7h 16m
│        │  Sat   ··  ··  ··  ··  ··  ··  ··  ··  ··  ··  ··  │      —
│        │  Sun   ··  ··  ··  ··  ··  ··  ··  ··  ··  ··  ··  │      —
│        │──────────────────────────────────────────────────────│
```

- Each day is a row. Blocks are color-coded by tag.
- Daily totals right-aligned in monospace.
- Click any day row to drill into the Day view.
- `··` = outside working hours. `░░` = gap within working hours.

---

## 7. Milestones

### Phase 1 — MVP (4-6 weeks)
- Timer controls: start, stop, cancel, continue
- Day timeline view with color-coded intervals
- Click-to-edit interval detail panel (modify times, tags, annotation)
- Basic tag autocomplete from `timew tags`
- System tray icon with active timer display

### Phase 2 — Core Experience (4-6 weeks)
- Week timeline view
- Drag-to-resize and drag-to-move intervals
- Summary table with date range picker and tag filter
- Gap visualization and quick-fill
- Undo support
- Dark/Light mode

### Phase 3 — Analytics & Polish (3-4 weeks)
- Dashboard with daily/weekly progress widgets
- Bar/pie charts for time distribution
- CSV and PDF export
- Tag color assignment and management
- Daily/weekly hour targets
- Global hotkeys

### Phase 4 — Advanced (Ongoing)
- Idle detection and prompts
- Notification reminders
- Extension report integration
- Split and join operations on timeline
- Multi-interval bulk editing
- Keyboard-driven workflow (vim-like shortcuts)

---

## 8. Success Metrics

| Metric | Target |
|--------|--------|
| Daily active users (if distributed) | 500+ within 6 months of release |
| CLI parity coverage | 90%+ of `timew` commands accessible via GUI |
| Start-to-tracking time | < 3 seconds from app launch |
| Crash rate | < 0.1% of sessions |
| GitHub stars (if open source) | 1000+ within first year |

---

## 9. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| `timew` CLI output format changes between versions | Data parsing breaks | Pin to known versions; use `export` JSON (stable format) |
| Performance with very large datasets (years of data) | Slow timeline rendering | Paginate data; only load visible date range from `timew export <range>` |
| JVM bundling increases app size (~60-80MB) | Larger download than native C/Rust app | Use `jlink` to create minimal custom JRE; Conveyor or jpackage for optimized packaging |
| Compose Multiplatform rendering differences across OS | Minor visual inconsistencies | Test on all 3 platforms in CI; use Skia renderer (consistent across platforms) |
| Users expect real-time sync with CLI changes | Stale GUI state | File watcher on `~/.local/share/timewarrior/data/` directory; auto-refresh on window focus |
| Tag color persistence across reinstalls | Lost customization | Store in `$XDG_CONFIG_HOME/timewgui/config.json`; offer import/export |
| Custom timeline Canvas rendering is complex | High initial dev effort | Build a reusable `TimelineComponent` with gesture handling early; invest in Phase 1 |

---

## 10. Open Questions

1. **Should the GUI support Timewarrior extensions?** Extensions are arbitrary scripts; exposing them adds complexity but increases power-user value.
2. **Should we support multiple Timewarrior databases?** The `TIMEWARRIORDB` env var allows switching databases — should the GUI allow switching?
3. **Integration with Taskwarrior?** Timewarrior has optional Taskwarrior integration (on-modify hook). Should the GUI surface task context?
4. **Localization?** Should the GUI be translatable from day one, or start English-only?
5. **Package distribution?** Homebrew, Flatpak, AUR, `.dmg`, `.deb`, `.msi` — which to prioritize?

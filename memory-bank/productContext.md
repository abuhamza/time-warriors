# Product Context

## Why This Exists
Timewarrior is a powerful CLI time tracker with no native GUI. Users must memorize commands, read terminal tables, and have no visual timeline or drag-to-edit capabilities — all standard in modern time-tracking tools.

## Problems Solved
1. Eliminates need to memorize `timew` CLI syntax
2. Provides visual timeline for understanding time allocation
3. Enables click/drag editing of intervals
4. Dashboard for at-a-glance daily/weekly progress
5. Tag color coding for visual differentiation

## How It Works
- Desktop app that wraps the `timew` CLI
- Reads data via `timew export` (JSON format)
- Writes data via `timew start/stop/modify/delete/etc.` commands
- GUI-specific config (tag colors, targets) stored separately via `java.util.prefs.Preferences`
- Task management with file-based JSON persistence (`~/.config/timewgui/tasks.json`), linked to intervals via generated tags
- Real-time timer display with 1-second tick updates
- Canvas-based timeline rendering for day and week views

## UX Goals
- Start tracking in < 3 seconds from app launch
- Warm, inviting African savanna aesthetic (earth tones, amber accent)
- Full keyboard navigation
- Information-dense layout with collapsible sidebar
- Monospace typography for times/durations for precise alignment

## Branding
- App name: **TimewGUI** (window title)
- Sidebar brand: **TimeTrackAI** (displayed to user)
- Custom savanna sunset banner on dashboard for visual identity

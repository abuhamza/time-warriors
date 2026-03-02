# TimewGUI — Project Brief

## Core Purpose
A modern Compose Desktop GUI for the Timewarrior CLI (`timew`), enabling visual time tracking without memorizing CLI syntax while preserving full CLI data compatibility.

## Key Goals
- Warm, inviting UI with African savanna-inspired design language
- All mutations go through `timew` CLI commands (never write data files directly)
- Cross-platform: macOS (primary), Linux, Windows via Compose Multiplatform
- Phase 1 MVP: Timer controls, day/week timeline, interval editing, tag autocomplete, dashboard

## Target Users
- Developers already using Timewarrior who want visual insight
- Teams wanting lower barrier to Timewarrior adoption
- Users who prefer GUIs but value Timewarrior's data model

## Non-Goals
- Not a CLI replacement — a complement
- Not a SaaS/cloud service — local desktop app
- Not reimplementing Timewarrior's storage engine

## Current Status
Phase 1 MVP is feature-complete with Task Management added beyond the original PRD scope. The application compiles and runs (33 Kotlin files, ~5,146 lines). In the polish and testing phase before moving to Phase 2 features (drag editing, gap fill, undo, system tray).

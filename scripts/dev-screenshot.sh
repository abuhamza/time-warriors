#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SCREENSHOTS_DIR="$PROJECT_ROOT/screenshots"
HISTORY_DIR="$SCREENSHOTS_DIR/history"
VENV_PYTHON="$PROJECT_ROOT/.dev/venv/bin/python3"

mkdir -p "$HISTORY_DIR"

if [[ ! -x "$VENV_PYTHON" ]]; then
    echo "Setting up Python venv for Quartz ..." >&2
    python3 -m venv "$PROJECT_ROOT/.dev/venv"
    "$PROJECT_ROOT/.dev/venv/bin/pip" install -q pyobjc-framework-Quartz
fi

TARGET_PID=""
TARGET_TITLE="TimewGUI"
PROBE_ONLY=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --pid)    TARGET_PID="$2"; shift 2 ;;
        --title)  TARGET_TITLE="$2"; shift 2 ;;
        --probe)  PROBE_ONLY=true; shift ;;
        *)        echo "Unknown flag: $1" >&2; exit 1 ;;
    esac
done

find_window_id() {
    "$VENV_PYTHON" -c "
import Quartz, sys

target_pid = int('${TARGET_PID}') if '${TARGET_PID}' else None
target_title = '${TARGET_TITLE}'

windows = Quartz.CGWindowListCopyWindowInfo(
    Quartz.kCGWindowListOptionOnScreenOnly,
    Quartz.kCGNullWindowID
)

# Pass 1: match by PID (layer-0 windows only)
for w in windows:
    layer = w.get('kCGWindowLayer', 999)
    if layer != 0:
        continue
    if target_pid and w.get('kCGWindowOwnerPID') == target_pid:
        print(w['kCGWindowNumber'])
        sys.exit(0)

# Pass 2: match by window/owner name
for w in windows:
    layer = w.get('kCGWindowLayer', 999)
    if layer != 0:
        continue
    name = str(w.get('kCGWindowName', ''))
    owner = str(w.get('kCGWindowOwnerName', ''))
    if target_title in name or target_title in owner:
        print(w['kCGWindowNumber'])
        sys.exit(0)

sys.exit(1)
"
}

check_screen_recording_permission() {
    if screencapture -x /tmp/.timewgui-perm-check.png 2>/dev/null; then
        rm -f /tmp/.timewgui-perm-check.png
        return 0
    fi
    return 1
}

WINDOW_ID=$(find_window_id 2>/dev/null) || WINDOW_ID=""

if $PROBE_ONLY; then
    if [[ -n "$WINDOW_ID" ]]; then
        echo "$WINDOW_ID"
        exit 0
    else
        exit 1
    fi
fi

if ! check_screen_recording_permission; then
    echo "ERROR: Screen Recording permission required." >&2
    echo "" >&2
    echo "Grant permission to Cursor (or your terminal app):" >&2
    echo "  System Settings → Privacy & Security → Screen Recording → enable Cursor" >&2
    echo "" >&2
    echo "Opening System Settings ..." >&2
    open "x-apple.systempreferences:com.apple.preference.security?Privacy_ScreenCapture" 2>/dev/null || true
    exit 2
fi

TIMESTAMP=$(date -u +"%Y%m%dT%H%M%SZ")
LATEST="$SCREENSHOTS_DIR/latest.png"
HISTORY_FILE="$HISTORY_DIR/$TIMESTAMP.png"

if [[ -n "$WINDOW_ID" ]]; then
    screencapture -x -l "$WINDOW_ID" "$LATEST"
    cp "$LATEST" "$HISTORY_FILE"
    echo "Screenshot saved: $LATEST"
    echo "History copy:     $HISTORY_FILE"
else
    echo "Warning: No matching window found. Falling back to full-screen capture." >&2
    screencapture -x "$LATEST"
    cp "$LATEST" "$HISTORY_FILE"
    echo "Full-screen screenshot saved: $LATEST"
    echo "History copy:                 $HISTORY_FILE"
fi

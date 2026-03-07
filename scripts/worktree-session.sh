#!/usr/bin/env bash
#
# worktree-session.sh — Manage git worktrees for parallel Claude Code sessions
#
# Usage:
#   ./scripts/worktree-session.sh new <branch-name> [base-branch]  Create worktree + launch Claude
#   ./scripts/worktree-session.sh list                              List active worktrees
#   ./scripts/worktree-session.sh remove <branch-name>              Remove a worktree
#   ./scripts/worktree-session.sh clean                             Remove all worktrees

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
WORKTREE_DIR="$REPO_ROOT/.worktrees"

# Files/directories to copy into each worktree (relative to repo root).
# These are untracked configs that Claude and other tools need.
COPY_CONFIGS=(
    .claude           # Claude Code settings (settings.json, settings.local.json)
    .cursor/rules     # Cursor AI rules
    .env              # Environment variables (if present)
    .env.local        # Local env overrides (if present)
    local.properties  # Gradle local properties (if present)
)

copy_configs() {
    local wt_path="$1"

    for item in "${COPY_CONFIGS[@]}"; do
        # Strip inline comments
        item="${item%%#*}"
        item="${item%"${item##*[![:space:]]}"}"  # trim trailing whitespace
        [ -z "$item" ] && continue

        local src="$REPO_ROOT/$item"
        [ -e "$src" ] || continue

        local dest="$wt_path/$item"
        mkdir -p "$(dirname "$dest")"

        if [ -d "$src" ]; then
            cp -r "$src" "$dest"
        else
            cp "$src" "$dest"
        fi
        echo "  Copied $item"
    done
}

usage() {
    sed -n '3,8p' "$0" | sed 's/^# \?//'
    exit 1
}

cmd_new() {
    local branch="${1:?Usage: $0 new <branch-name> [base-branch]}"
    local base="${2:-HEAD}"

    mkdir -p "$WORKTREE_DIR"

    local wt_path="$WORKTREE_DIR/$branch"

    if [ -d "$wt_path" ]; then
        echo "Worktree already exists at $wt_path"
        echo "Launching Claude Code session..."
        claude --cwd "$wt_path"
        return
    fi

    echo "Creating worktree for branch '$branch' from '$base'..."
    git worktree add "$wt_path" -b "$branch" "$base"

    echo "Copying config files..."
    copy_configs "$wt_path"

    # Auto-detect and run project setup
    if [ -f "$wt_path/package.json" ]; then
        echo "Running npm install..."
        (cd "$wt_path" && npm install)
    elif [ -f "$wt_path/build.gradle.kts" ] || [ -f "$wt_path/build.gradle" ]; then
        echo "Gradle project detected — dependencies will resolve on first build."
    elif [ -f "$wt_path/Cargo.toml" ]; then
        echo "Running cargo build..."
        (cd "$wt_path" && cargo build)
    elif [ -f "$wt_path/requirements.txt" ]; then
        echo "Running pip install..."
        (cd "$wt_path" && pip install -r requirements.txt)
    elif [ -f "$wt_path/go.mod" ]; then
        echo "Running go mod download..."
        (cd "$wt_path" && go mod download)
    fi

    echo ""
    echo "Worktree ready at: $wt_path"
    echo "Launching Claude Code session..."
    echo ""
    claude --cwd "$wt_path"
}

cmd_list() {
    echo "Active worktrees:"
    echo ""
    git worktree list
}

cmd_remove() {
    local branch="${1:?Usage: $0 remove <branch-name>}"
    local wt_path="$WORKTREE_DIR/$branch"

    if [ ! -d "$wt_path" ]; then
        echo "No worktree found at $wt_path"
        exit 1
    fi

    echo "Removing worktree at $wt_path..."
    git worktree remove "$wt_path"
    echo "Deleting branch '$branch'..."
    git branch -d "$branch" 2>/dev/null || git branch -D "$branch"
    echo "Done."
}

cmd_clean() {
    if [ ! -d "$WORKTREE_DIR" ]; then
        echo "No worktrees directory found."
        exit 0
    fi

    local count
    count=$(find "$WORKTREE_DIR" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | wc -l)

    if [ "$count" -eq 0 ]; then
        echo "No worktrees to clean."
        exit 0
    fi

    echo "This will remove $count worktree(s) under $WORKTREE_DIR:"
    find "$WORKTREE_DIR" -mindepth 1 -maxdepth 1 -type d -exec basename {} \;
    echo ""
    read -rp "Continue? [y/N] " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        echo "Aborted."
        exit 0
    fi

    for wt in "$WORKTREE_DIR"/*/; do
        [ -d "$wt" ] || continue
        local name
        name="$(basename "$wt")"
        echo "Removing $name..."
        git worktree remove "$wt" --force 2>/dev/null || rm -rf "$wt"
        git branch -d "$name" 2>/dev/null || git branch -D "$name" 2>/dev/null || true
    done

    git worktree prune
    echo "All worktrees cleaned."
}

# --- Parallel session launcher ---
cmd_parallel() {
    shift_args=("$@")
    if [ ${#shift_args[@]} -lt 2 ]; then
        echo "Usage: $0 parallel <branch1> <branch2> [branch3] ..."
        echo ""
        echo "Creates multiple worktrees and launches Claude Code in separate terminals."
        exit 1
    fi

    for branch in "${shift_args[@]}"; do
        local wt_path="$WORKTREE_DIR/$branch"
        mkdir -p "$WORKTREE_DIR"

        if [ ! -d "$wt_path" ]; then
            echo "Creating worktree for '$branch'..."
            git worktree add "$wt_path" -b "$branch" HEAD
            copy_configs "$wt_path"
        fi
    done

    echo ""
    echo "All worktrees created. Launching Claude sessions..."
    echo ""

    for branch in "${shift_args[@]}"; do
        local wt_path="$WORKTREE_DIR/$branch"
        echo "  Launching Claude in terminal for: $branch"

        if command -v gnome-terminal &>/dev/null; then
            gnome-terminal --title="Claude: $branch" -- bash -c "cd '$wt_path' && claude; exec bash" &
        elif command -v xterm &>/dev/null; then
            xterm -title "Claude: $branch" -e "cd '$wt_path' && claude; exec bash" &
        elif command -v tmux &>/dev/null; then
            tmux new-window -n "$branch" "cd '$wt_path' && claude"
        else
            echo "    No supported terminal found (gnome-terminal, xterm, tmux)."
            echo "    Open a terminal manually and run: cd '$wt_path' && claude"
        fi
    done

    echo ""
    echo "Sessions launched. Use '$0 list' to see worktrees."
}

case "${1:-}" in
    new)      shift; cmd_new "$@" ;;
    list)     cmd_list ;;
    remove)   shift; cmd_remove "$@" ;;
    clean)    cmd_clean ;;
    parallel) shift; cmd_parallel "$@" ;;
    *)        usage ;;
esac

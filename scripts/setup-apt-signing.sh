#!/usr/bin/env bash
set -euo pipefail

# Setup GPG signing key, store in 1Password, add to GitHub secrets,
# initialize gh-pages branch, and enable GitHub Pages.
#
# Usage: bash scripts/setup-apt-signing.sh

REPO="abuhamza/time-warriors"
KEY_NAME="TimewGUI APT Signing Key"
KEY_EMAIL="abuhamza@users.noreply.github.com"
TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT
PUBLIC_KEY="$TMP_DIR/timewgui-public.key"

# ── Helpers ──────────────────────────────────────────────────────────

bold() { printf '\033[1m%s\033[0m\n' "$*"; }
green() { printf '\033[32m✓ %s\033[0m\n' "$*"; }
yellow() { printf '\033[33m⚠ %s\033[0m\n' "$*"; }
red() { printf '\033[31m✗ %s\033[0m\n' "$*"; exit 1; }

confirm() {
  read -rp "$1 [Y/n] " ans
  case "${ans:-Y}" in
    [Yy]*) return 0 ;;
    *) return 1 ;;
  esac
}

# ── Step 0: Check prerequisites ─────────────────────────────────────

bold "Step 0: Checking prerequisites..."

for cmd in gpg gh git; do
  command -v "$cmd" >/dev/null || red "$cmd is required but not installed"
  green "$cmd found"
done

if command -v op >/dev/null; then
  green "1Password CLI found"
  HAS_OP=true
else
  yellow "1Password CLI (op) not found — will skip 1Password storage"
  yellow "Install later: https://developer.1password.com/docs/cli/"
  HAS_OP=false
fi

echo ""

# ── Step 1: Generate GPG key ────────────────────────────────────────

bold "Step 1: Generating GPG signing key..."

if gpg --list-keys "$KEY_NAME" &>/dev/null; then
  yellow "GPG key '$KEY_NAME' already exists — skipping generation"
else
  gpg --batch --gen-key <<EOF
Key-Type: RSA
Key-Length: 4096
Name-Real: $KEY_NAME
Name-Email: $KEY_EMAIL
Expire-Date: 0
%no-protection
EOF
  green "GPG key generated"
fi

# Export keys
gpg --export --armor "$KEY_NAME" > "$PUBLIC_KEY"
green "Public key exported to $PUBLIC_KEY"

PRIVATE_KEY=$(gpg --export-secret-keys --armor "$KEY_NAME")
green "Private key exported to memory"

echo ""

# ── Step 2: Store in 1Password ───────────────────────────────────────

bold "Step 2: Storing private key in 1Password..."

if [ "$HAS_OP" = true ]; then
  if op item get "TimewGUI GPG Signing Key" &>/dev/null; then
    yellow "1Password item 'TimewGUI GPG Signing Key' already exists — skipping"
  else
    PRIVATE_KEY_FILE="$TMP_DIR/private.asc"
    echo "$PRIVATE_KEY" > "$PRIVATE_KEY_FILE"
    op item create \
      --category=SecureNote \
      --title="TimewGUI GPG Signing Key" \
      "notesPlain=$(cat "$PRIVATE_KEY_FILE")"
    rm -f "$PRIVATE_KEY_FILE"
    green "Private key stored in 1Password"
  fi
else
  yellow "Skipping 1Password storage (op not installed)"
  echo "  Save your private key manually. You can retrieve it later with:"
  echo "    gpg --export-secret-keys --armor \"$KEY_NAME\""
fi

echo ""

# ── Step 3: Add to GitHub Actions secrets ────────────────────────────

bold "Step 3: Adding private key to GitHub Actions secrets..."

echo "$PRIVATE_KEY" | gh secret set GPG_PRIVATE_KEY --repo "$REPO"
green "GPG_PRIVATE_KEY secret set on $REPO"

echo ""

# ── Step 4: Initialize gh-pages branch ───────────────────────────────

bold "Step 4: Initializing gh-pages branch..."

if git ls-remote --heads origin gh-pages | grep -q gh-pages; then
  yellow "gh-pages branch already exists on remote — skipping initialization"
else
  bash scripts/init-apt-repo.sh "$PUBLIC_KEY"
  green "gh-pages branch created and pushed"
fi

echo ""

# ── Step 5: Enable GitHub Pages ──────────────────────────────────────

bold "Step 5: Enabling GitHub Pages..."

if gh api "repos/$REPO/pages" &>/dev/null 2>&1; then
  yellow "GitHub Pages already enabled"
else
  if echo '{"source":{"branch":"gh-pages","path":"/"},"build_type":"legacy"}' | \
    gh api "repos/$REPO/pages" --method POST --input - 2>/dev/null; then
    green "GitHub Pages enabled for gh-pages branch"
  else
    yellow "Could not enable GitHub Pages via API"
    echo "  Enable manually: https://github.com/$REPO/settings/pages"
    echo "  Select: Source → Deploy from a branch → gh-pages → / (root) → Save"
  fi
fi

echo ""

# ── Done ─────────────────────────────────────────────────────────────

bold "═══════════════════════════════════════════════"
bold " Setup complete!"
bold "═══════════════════════════════════════════════"
echo ""
echo "Next steps:"
echo "  1. Wait ~60s for GitHub Pages to deploy"
echo "  2. Verify: curl -sI https://abuhamza.github.io/time-warriors/"
echo "  3. Push code:  git push origin main"
echo "  4. Release:    make ship v=X.Y.Z m=\"Your message\""
echo ""
echo "After a release, verify the APT repo:"
echo "  curl -s https://abuhamza.github.io/time-warriors/dists/stable/main/binary-amd64/Packages | head -20"
echo ""
echo "Test install (Docker):"
echo "  docker run --rm -it ubuntu:24.04 bash -c '"
echo "    apt-get update && apt-get install -y curl gnupg &&"
echo "    curl -fsSL https://abuhamza.github.io/time-warriors/public.key | gpg --dearmor -o /usr/share/keyrings/timewgui.gpg &&"
echo "    echo \"deb [signed-by=/usr/share/keyrings/timewgui.gpg arch=amd64] https://abuhamza.github.io/time-warriors stable main\" > /etc/apt/sources.list.d/timewgui.list &&"
echo "    apt-get update && apt-cache show timewgui"
echo "  '"

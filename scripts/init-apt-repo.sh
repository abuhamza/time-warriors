#!/usr/bin/env bash
set -euo pipefail

# Initialize the gh-pages branch with APT repo skeleton and landing page.
# Run once: bash scripts/init-apt-repo.sh <path-to-public.key>

PUBLIC_KEY="${1:?Usage: $0 <path-to-public.key>}"

if [ ! -f "$PUBLIC_KEY" ]; then
  echo "ERROR: Public key file not found: $PUBLIC_KEY" >&2
  exit 1
fi

REPO_URL="https://abuhamza.github.io/time-warriors"

# Resolve absolute path before we cd into the worktree
PUBLIC_KEY="$(realpath "$PUBLIC_KEY")"

# Create an orphan gh-pages branch in a temp worktree
WORK=$(mktemp -d)
trap 'git worktree remove "$WORK" 2>/dev/null || true; rm -rf "$WORK"' EXIT

git worktree add --detach "$WORK"
cd "$WORK"
git checkout --orphan gh-pages
git rm -rf . 2>/dev/null || true

# Create directory structure
mkdir -p dists/stable/main/binary-amd64
mkdir -p pool/main/t/timewgui

# Copy public key
cp "$PUBLIC_KEY" public.key

# Create placeholder Packages file (empty until first release)
touch dists/stable/main/binary-amd64/Packages
gzip -k dists/stable/main/binary-amd64/Packages

# Create landing page
cat > index.html << 'HTMLEOF'
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>TimewGUI APT Repository</title>
  <style>
    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; max-width: 700px; margin: 60px auto; padding: 0 20px; color: #333; line-height: 1.6; }
    h1 { color: #8B4513; }
    pre { background: #1e1e1e; color: #d4d4d4; padding: 16px; border-radius: 8px; overflow-x: auto; font-size: 14px; }
    code { font-family: "SF Mono", "Fira Code", monospace; }
    a { color: #8B4513; }
  </style>
</head>
<body>
  <h1>TimewGUI APT Repository</h1>
  <p>Install <a href="https://github.com/abuhamza/time-warriors">TimewGUI</a> on Debian/Ubuntu via apt:</p>
  <pre><code># Add the signing key
curl -fsSL https://abuhamza.github.io/time-warriors/public.key \
  | sudo gpg --dearmor -o /usr/share/keyrings/timewgui.gpg

# Add the repository
echo "deb [signed-by=/usr/share/keyrings/timewgui.gpg arch=amd64] \
  https://abuhamza.github.io/time-warriors stable main" \
  | sudo tee /etc/apt/sources.list.d/timewgui.list

# Install
sudo apt update &amp;&amp; sudo apt install timewgui</code></pre>
  <p><strong>Requires:</strong> <code>timewarrior</code> (installed automatically as a dependency).</p>
</body>
</html>
HTMLEOF

# Disable Jekyll processing (GitHub Pages)
touch .nojekyll

# Commit and push
git add -A
git commit -m "Initialize APT repository structure"
git push origin gh-pages

cd -

echo "Done. Enable GitHub Pages for gh-pages branch at:"
echo "  https://github.com/abuhamza/time-warriors/settings/pages"

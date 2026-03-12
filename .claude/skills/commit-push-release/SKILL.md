---
name: commit-push-release
description: Commit, push, and release (tag + push tag to trigger CI release)
allowed-tools: Bash(git add:*), Bash(git status:*), Bash(git commit:*), Bash(git push:*), Bash(git tag:*), Bash(git log:*), Bash(git diff:*)
---

## Context

- Current git status: !`git status`
- Current git diff (staged and unstaged changes): !`git diff HEAD`
- Current branch: !`git branch --show-current`
- Recent commits: !`git log --oneline -10`
- Current version in gradle.properties: !`grep appVersion gradle.properties 2>/dev/null || echo "not found"`

## Your task

Based on the above changes, commit, push, and create a release tag. Follow these steps:

1. **Stage and commit** — If there are uncommitted changes, stage them and create a single commit with an appropriate conventional-commit message. If the working tree is clean, skip this step.
2. **Push to origin** — Push the current branch to origin.
3. **Tag and push tag** — Ask the user what version to tag (suggest bumping from the current version). Then create the git tag `vX.Y.Z` and push it to origin. The tag push triggers CI to build packages and create the GitHub release.

IMPORTANT:
- Always push the branch BEFORE creating/pushing the tag.
- Use lightweight tags (no -a flag).
- Format: `git tag "vX.Y.Z"` then `git push origin "vX.Y.Z"`.
- If no version is provided by the user, suggest one based on the nature of changes (patch for fixes, minor for features, major for breaking changes).

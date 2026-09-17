#!/usr/bin/env bash
# SOURCE OF TRUTH: architectural choke points. ServiceLocator.kt and
# ZenModeApp.kt's onCreate() are the two files that explain the whole
# composite-build/backend-selection story - keep the
# [WHAT]/[WHY]/[HOW]/[WHERE] tags in them so that context is found by grep
# instead of requiring the private docs.
#
# Single source of truth for this check's logic - called identically by CI,
# the pre-commit hook, the Claude Code PostToolUse hook, and the guardrail
# self-tests.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CHOKE_POINT_FILES="$REPO_ROOT/core-api/src/main/java/com/zenlauncher/zenmode/coreapi/services/ServiceLocator.kt $REPO_ROOT/app/src/main/java/com/zenlauncher/zenmode/ZenModeApp.kt"

fail=0
for f in $CHOKE_POINT_FILES; do
  [ -f "$f" ] || continue
  for tag in '\[WHAT\]' '\[WHY\]' '\[HOW\]' '\[WHERE\]'; do
    if ! grep -qE "$tag" "$f"; then
      echo "error: $f is missing a $tag micro-context tag." >&2
      fail=1
    fi
  done
done

exit "$fail"

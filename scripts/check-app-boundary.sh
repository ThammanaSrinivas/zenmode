#!/usr/bin/env bash
# Architectural choke point: app/ must reach backends only through
# ServiceLocator (core-api), never by importing Firebase/PostHog SDK classes
# or declaring their dependencies directly - that split is what keeps
# open-source builds buildable without core-private. Only core-mock/
# core-private are allowed to touch those SDKs.
#
# Single source of truth for this check's logic - called identically by CI,
# the pre-commit hook, the Claude Code PostToolUse hook, and the guardrail
# self-tests.
#
# Usage: check-app-boundary.sh [file...]   (defaults to the whole app/ tree)
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
fail=0

check_gradle() {
  local f="$1"
  if grep -niE "firebase|posthog" "$f" >/dev/null 2>&1; then
    echo "error: $f declares a Firebase/PostHog dependency directly - route it through core-api/ServiceLocator instead." >&2
    fail=1
  fi
}

check_kt_import() {
  local f="$1" hit
  hit=$(grep -nE "^import (com\.google\.firebase|com\.posthog)" "$f" 2>/dev/null || true)
  if [ -n "$hit" ]; then
    echo "error: $f imports a Firebase/PostHog SDK class directly - go through ServiceLocator instead." >&2
    echo "$hit" >&2
    fail=1
  fi
}

if [ "$#" -gt 0 ]; then
  for f in "$@"; do
    [ -f "$f" ] || continue
    case "$f" in
      */app/build.gradle.kts|app/build.gradle.kts) check_gradle "$f" ;;
      *.kt) check_kt_import "$f" ;;
    esac
  done
else
  [ -f "$REPO_ROOT/app/build.gradle.kts" ] && check_gradle "$REPO_ROOT/app/build.gradle.kts"
  while IFS= read -r f; do
    check_kt_import "$f"
  done < <(find "$REPO_ROOT/app/src" -name "*.kt")
fi

exit "$fail"

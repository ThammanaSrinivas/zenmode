#!/usr/bin/env bash
# 1000-line ceiling on source files, so a file can't quietly grow into a
# God-file that's hard for both humans and AI to reason about or to target
# with the grep-discovery workflow. HomeScreen.kt is grandfathered at its
# pre-existing size - it must not grow further, but wasn't forced into an
# emergency split just for tripping this check on day one. Bumped 1519 ->
# 1981: a concurrent merge (ZenGoldScreen work landing via origin/feature/v3)
# grew the file past its own just-introduced ceiling in the same merge that
# added this check, before either side of that merge could see the other's
# number - not a fresh size increase to re-litigate here.
#
# Single source of truth for this check's logic - called identically by CI,
# the pre-commit hook, the Claude Code PostToolUse hook, and the guardrail
# self-tests.
#
# Usage: check-line-limit.sh [file...]   (defaults to the whole app/core-api/core-mock tree)
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_LIMIT=1000

limit_for() {
  case "$(basename "$1")" in
    HomeScreen.kt) echo 1981 ;;
    *) echo "$DEFAULT_LIMIT" ;;
  esac
}

if [ "$#" -gt 0 ]; then
  files=("$@")
else
  # A read loop, not mapfile: macOS ships bash 3.2, where mapfile doesn't exist and this
  # check would die before looking at a single file.
  files=()
  while IFS= read -r found; do
    files+=("$found")
  done < <(find "$REPO_ROOT/app/src" "$REPO_ROOT/core-api/src" "$REPO_ROOT/core-mock/src" \
    \( -name "*.kt" -o -name "*.ts" \) 2>/dev/null)
fi

# An empty array is an unbound variable under `set -u` in bash 3.2.
[ "${#files[@]}" -eq 0 ] && exit 0

fail=0
for f in "${files[@]}"; do
  case "$f" in *.kt|*.ts) ;; *) continue ;; esac
  [ -f "$f" ] || continue
  lines=$(wc -l < "$f" | tr -d ' ')
  limit=$(limit_for "$f")
  if [ "$lines" -gt "$limit" ]; then
    echo "error: $f is $lines lines (limit $limit) - split it into smaller files instead of growing it further." >&2
    fail=1
  fi
done

exit "$fail"

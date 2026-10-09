#!/usr/bin/env bash
# A Compose InfiniteTransition asks the frame clock for a new frame every vsync
# for as long as it exists - whether or not anything reads its value. So a
# transition created unconditionally keeps the UI thread waking ~60-120x a
# second even for a user who has reduced motion turned on and sees nothing
# move. On a launcher, whose home screen is on screen more than any other app's
# UI, that is the single easiest battery mistake to make and the hardest to
# notice, because nothing looks wrong.
#
# The rule: a file that creates an InfiniteTransition must also consult
# rememberReduceMotion, so the transition isn't built when motion is off.
#
# This is a smoke check, not a proof - it can't tell that every call site in a
# file is guarded, only that the file knows the concept exists. It's here to
# make the next person think about reduced motion, which is the part that was
# actually missing.
#
# Files in motion-guard-debt.txt predate the check. They're transient surfaces
# (sheets, onboarding steps) rather than always-on backdrops, so they weren't
# worth a risky retrofit on day one - but they must not grow in number, and
# shrinking the list is welcome.
#
# Single source of truth for this check's logic - called identically by CI,
# the pre-commit hook, and the guardrail self-tests.
#
# Usage: check-motion-guards.sh [file...]   (defaults to the whole app tree)
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEBT_FILE="$REPO_ROOT/scripts/motion-guard-debt.txt"

is_debt() {
  [ -f "$DEBT_FILE" ] || return 1
  grep -qxF "$1" <(grep -vE '^\s*(#|$)' "$DEBT_FILE")
}

# No mapfile/readarray here: macOS still ships bash 3.2, and this script runs
# from the pre-commit hook on developer machines as well as in CI. Newline-
# delimited and read with IFS= rather than iterated as a word list, because the
# checkout path itself can contain spaces ("ZenMode OS/") - splitting on those
# made an earlier version of this check silently pass without reading a file.
if [ "$#" -gt 0 ]; then
  files="$(printf '%s\n' "$@")"
else
  files="$(grep -rl "rememberInfiniteTransition" \
    --include="*.kt" "$REPO_ROOT/app/src/main/java" 2>/dev/null | sort)"
fi

failed=0
checked=0
while IFS= read -r file; do
  [ -n "$file" ] || continue
  [ -f "$file" ] || continue
  grep -q "rememberInfiniteTransition" "$file" || continue
  checked=$((checked + 1))

  rel="${file#"$REPO_ROOT"/}"
  if grep -q "rememberReduceMotion" "$file"; then
    continue
  fi
  if is_debt "$rel"; then
    continue
  fi

  echo "$rel: creates an InfiniteTransition but never consults rememberReduceMotion."
  echo "    An InfiniteTransition requests a frame every vsync for as long as it exists,"
  echo "    even when nothing reads it, so this burns battery animating nothing for users"
  echo "    who asked for reduced motion. Gate it:"
  echo
  echo "        val still = rememberReduceMotion() || LocalInspectionMode.current"
  echo "        val t: State<Float>"
  echo "        if (still) {"
  echo "            t = remember { mutableStateOf(<settled value>) }"
  echo "        } else {"
  echo "            t = rememberInfiniteTransition(label = \"…\").animateFloat(…)"
  echo "        }"
  echo
  echo "    See MoodBackdrop.kt for the pattern. If this surface genuinely has to be"
  echo "    exempt, add its path to scripts/motion-guard-debt.txt with a reason."
  echo
  failed=1
done <<EOF
$files
EOF

if [ "$failed" -ne 0 ]; then
  echo "motion-guard check failed."
  exit 1
fi

# A check that silently inspected nothing is worse than no check at all - it
# reports success forever. Only the explicit-file form is allowed to find none.
if [ "$checked" -eq 0 ] && [ "$#" -eq 0 ]; then
  echo "motion-guard check found no files to inspect - the check itself is broken." >&2
  exit 1
fi

echo "motion-guard check passed ($checked file(s) inspected)."

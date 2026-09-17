#!/usr/bin/env bash
# Tests the guardrails themselves, not the app: plants a known-bad fixture,
# asserts the corresponding scripts/*.sh check actually catches it, then
# removes the fixture and asserts a clean pass. Without this, a guardrail can
# be silently weakened (a typo'd regex, an over-loosened pattern) and nobody
# finds out until the thing it was supposed to catch ships anyway.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$REPO_ROOT"

fail=0
pass=0

expect_fail() {
  local desc="$1" script="$2"; shift 2
  if "$script" "$@" >/tmp/guardrail_test_out.$$ 2>&1; then
    echo "FAIL (should have caught violation): $desc" >&2
    cat /tmp/guardrail_test_out.$$ >&2
    fail=1
  else
    echo "ok: $desc catches the violation"
    pass=$((pass + 1))
  fi
  rm -f /tmp/guardrail_test_out.$$
}

expect_pass() {
  local desc="$1" script="$2"; shift 2
  if "$script" "$@" >/tmp/guardrail_test_out.$$ 2>&1; then
    echo "ok: $desc passes clean"
    pass=$((pass + 1))
  else
    echo "FAIL (false positive on clean state): $desc" >&2
    cat /tmp/guardrail_test_out.$$ >&2
    fail=1
  fi
  rm -f /tmp/guardrail_test_out.$$
}

# --- check-app-boundary.sh: Firebase/PostHog import in app/ ---
KT_FIXTURE="app/src/main/java/com/zenlauncher/zenmode/__guardrail_fixture.kt"
cat > "$KT_FIXTURE" <<'EOF'
package com.zenlauncher.zenmode
import com.google.firebase.firestore.FirebaseFirestore
EOF
expect_fail "check-app-boundary.sh (import)" ./scripts/check-app-boundary.sh "$KT_FIXTURE"
rm -f "$KT_FIXTURE"

# --- check-app-boundary.sh: Firebase/PostHog dep in app/build.gradle.kts ---
# check_gradle() matches on the "*/app/build.gradle.kts" path shape, so the
# fixture's parent directory must literally be named "app" - exercise it via
# a copy placed at that shape instead of touching the real file.
mkdir -p /tmp/__guardrail_fixture/app
cp app/build.gradle.kts /tmp/__guardrail_fixture/app/build.gradle.kts
echo '    implementation("com.google.firebase:firebase-firestore")' >> /tmp/__guardrail_fixture/app/build.gradle.kts
expect_fail "check-app-boundary.sh (gradle dep)" ./scripts/check-app-boundary.sh /tmp/__guardrail_fixture/app/build.gradle.kts
rm -rf /tmp/__guardrail_fixture

expect_pass "check-app-boundary.sh (clean repo)" ./scripts/check-app-boundary.sh

# --- check-choke-point-tags.sh --- (edits the real tracked file in place to
# simulate a stripped tag, so restore it via trap even if this is interrupted)
SL="core-api/src/main/java/com/zenlauncher/zenmode/coreapi/services/ServiceLocator.kt"
cp "$SL" /tmp/__guardrail_sl_backup.kt
restore_sl() { cp /tmp/__guardrail_sl_backup.kt "$SL" 2>/dev/null; rm -f /tmp/__guardrail_sl_backup.kt; }
trap restore_sl EXIT
sed -i 's/\[WHY\]//' "$SL"
expect_fail "check-choke-point-tags.sh" ./scripts/check-choke-point-tags.sh
restore_sl
trap - EXIT
expect_pass "check-choke-point-tags.sh (clean repo)" ./scripts/check-choke-point-tags.sh

# --- check-line-limit.sh ---
BIG_FIXTURE="app/src/main/java/com/zenlauncher/zenmode/__guardrail_fixture_big.kt"
{ echo "package com.zenlauncher.zenmode"; for i in $(seq 1 1005); do echo "// line $i"; done; } > "$BIG_FIXTURE"
expect_fail "check-line-limit.sh" ./scripts/check-line-limit.sh "$BIG_FIXTURE"
rm -f "$BIG_FIXTURE"
expect_pass "check-line-limit.sh (clean repo, HomeScreen.kt grandfathered)" ./scripts/check-line-limit.sh

echo ""
echo "$pass check(s) passed"
if [ "$fail" -eq 1 ]; then
  echo "One or more guardrails failed to catch a planted violation, or false-positived on clean state - see above." >&2
  exit 1
fi

exit 0

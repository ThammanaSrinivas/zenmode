#!/usr/bin/env bash
# Before/after battery measurement for a real device, so a change that's meant
# to save battery has to prove it instead of being argued for.
#
# The numbers that matter for this app are the screen-off ones. ZenMode is a
# launcher, so screen-on drain is dominated by the display and can't be
# attributed to us with any confidence; standby drain can, and standby is what
# a user means when they say an app "killed my battery".
#
# Usage:
#   scripts/battery-probe.sh reset              # start a measurement window
#   ... leave the phone alone, screen off, for a few hours ...
#   scripts/battery-probe.sh report [label]     # dump + summarise that window
#
#   scripts/battery-probe.sh report before      # the A/B pattern: baseline,
#   scripts/battery-probe.sh diff before after  # install the change, measure
#                                               # again, then compare
#
# Reports land in build/battery/<label>.txt (gitignored build dir) and are
# written in full, so the summary never hides something the raw dump saw.
set -uo pipefail

PKG="${ZENMODE_PKG:-com.zenlauncher.zenmode}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="$REPO_ROOT/build/battery"

die() { echo "error: $*" >&2; exit 1; }

require_device() {
  command -v adb >/dev/null 2>&1 || die "adb not on PATH (add \$ANDROID_HOME/platform-tools)"
  local count
  count="$(adb devices | awk 'NR>1 && $2=="device"' | wc -l | tr -d ' ')"
  [ "$count" = "1" ] || die "need exactly one attached device, found $count (check 'adb devices')"
  adb shell pm path "$PKG" >/dev/null 2>&1 || die "$PKG is not installed on the device"
}

# The app's UID is how batterystats attributes everything; without it the dump
# is the whole device and tells you nothing about this app.
app_uid() {
  adb shell dumpsys package "$PKG" 2>/dev/null \
    | awk -F= '/^[[:space:]]*userId=/ { gsub(/[^0-9]/, "", $2); print $2; exit }'
}

cmd_reset() {
  require_device
  # --enable full-wake-history keeps per-wakelock detail that the default dump
  # drops, which is the difference between "something woke the app" and knowing
  # what did.
  adb shell dumpsys batterystats --enable full-wake-history >/dev/null 2>&1
  adb shell dumpsys batterystats --reset >/dev/null 2>&1
  adb shell cmd stats clear-puller-cache >/dev/null 2>&1
  echo "battery stats reset for $PKG (uid $(app_uid))"
  echo
  echo "now, for a clean standby window:"
  echo "  1. unplug the device (charging suppresses the numbers you want)"
  echo "  2. screen off, app NOT in the foreground, left alone"
  echo "  3. same duration for every run you intend to compare"
  echo
  echo "then: scripts/battery-probe.sh report <label>"
}

cmd_report() {
  require_device
  local label="${1:-report}"
  local uid raw
  uid="$(app_uid)"
  [ -n "$uid" ] || die "could not resolve the uid for $PKG"

  mkdir -p "$OUT_DIR"
  raw="$OUT_DIR/$label.txt"

  {
    echo "# battery probe: $label"
    echo "# package: $PKG (uid $uid)"
    echo "# captured: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
    echo "# device: $(adb shell getprop ro.product.model 2>/dev/null | tr -d '\r') / Android $(adb shell getprop ro.build.version.release 2>/dev/null | tr -d '\r')"
    echo
    echo "## batterystats (checkin, this app only)"
    adb shell dumpsys batterystats --charged "$PKG" 2>/dev/null | tr -d '\r'
    echo
    echo "## procstats"
    adb shell dumpsys procstats "$PKG" 2>/dev/null | tr -d '\r'
    echo
    echo "## alarms + jobs"
    adb shell dumpsys alarm 2>/dev/null | grep -i "$PKG" | tr -d '\r'
    adb shell dumpsys jobscheduler 2>/dev/null | grep -i "$PKG" | tr -d '\r'
  } > "$raw"

  echo "raw dump: $raw"
  echo
  summarise "$raw" "$uid"
}

# Pulls out the handful of lines worth comparing between runs. Everything here
# is also in the raw dump - this is a reading aid, not a filter.
summarise() {
  local raw="$1" uid="$2"
  echo "── summary ─────────────────────────────────────────────"

  echo "window:"
  grep -E "Time on battery:|Time on battery screen off:" "$raw" | sed 's/^ */  /' || true

  echo "estimated drain attributed to this app:"
  grep -E "Estimated power use|Uid u?0?a?$uid:|Uid $uid:" "$raw" | head -5 | sed 's/^ */  /' || true

  echo "cpu:"
  grep -E "Total cpu time: |cpu=|Proc $PKG" "$raw" | head -8 | sed 's/^ */  /' || true

  echo "wakelocks (what held the device awake):"
  grep -E "Wake lock .*(realtime|partial)" "$raw" | head -10 | sed 's/^ */  /' || true

  echo "wakeups + jobs (how often something woke us):"
  grep -E "wakeup alarm|Alarm .*:|Job .*:|[0-9]+ wakeup" "$raw" | head -10 | sed 's/^ */  /' || true

  echo "notification listener / accessibility service presence:"
  grep -E "ZenNotificationListenerService|ZenAccessibilityService" "$raw" | head -5 | sed 's/^ */  /' || true

  echo "────────────────────────────────────────────────────────"
  echo "compare runs with: scripts/battery-probe.sh diff <before> <after>"
}

cmd_diff() {
  local a="$OUT_DIR/${1:?usage: diff <before> <after>}.txt"
  local b="$OUT_DIR/${2:?usage: diff <before> <after>}.txt"
  [ -f "$a" ] || die "no such report: $a"
  [ -f "$b" ] || die "no such report: $b"
  echo "comparing $1 -> $2"
  echo "(identical durations and device state, or this comparison means nothing)"
  echo
  diff <(grep -E "Time on battery|Total cpu time|Wake lock|wakeup alarm" "$a") \
       <(grep -E "Time on battery|Total cpu time|Wake lock|wakeup alarm" "$b") \
    && echo "no difference in the compared lines"
}

case "${1:-}" in
  reset)  shift; cmd_reset "$@" ;;
  report) shift; cmd_report "$@" ;;
  diff)   shift; cmd_diff "$@" ;;
  *) sed -n '2,20p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac

#!/usr/bin/env bash
# Duplicate-text guardrail: one spelling per meaning. "ZenMode OS" lives in
# AppConstants.PRODUCT_NAME (and every other `// ONE_SPELLING` const owns its
# text the same way); any other long string literal gets at most two copies
# before it has to become a named constant or a shared function. Existing
# repeats are grandfathered in scripts/duplicate-literal-debt.txt as a ratchet.
# The rules and thresholds are in scripts/check_duplicate_literals.py.
#
# Single source of truth for this check's logic - called identically by CI,
# the pre-commit hook, the Claude Code PostToolUse hook, and the guardrail
# self-tests.
#
# Usage: check-duplicate-literals.sh [file...]   (defaults to the whole app/core-api/core-mock tree)
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# python3 on Linux/macOS/CI, python or the py launcher on Windows (Git Bash).
for candidate in python3 python py; do
  if command -v "$candidate" >/dev/null 2>&1 && "$candidate" -c 'import sys; sys.exit(sys.version_info < (3, 8))' 2>/dev/null; then
    exec "$candidate" "$REPO_ROOT/scripts/check_duplicate_literals.py" "$REPO_ROOT" "$@"
  fi
done

# No Python 3.8+ here: say so and step aside rather than block a commit - CI always has one.
echo "warning: check-duplicate-literals.sh skipped - no Python 3.8+ found (python3/python/py)." >&2
exit 0

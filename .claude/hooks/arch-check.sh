#!/usr/bin/env bash
# Stop: before Claude finishes, compile and run the architecture rules if Java sources changed.
# Exit 2 sends the failure back to Claude, so it cannot finish with broken architecture rules.
set -uo pipefail
input=$(cat)
grep -Eq '"stop_hook_active"[[:space:]]*:[[:space:]]*true' <<<"$input" && exit 0   # avoid loops
cd "$CLAUDE_PROJECT_DIR"
[[ -z "$(git status --porcelain -- src)" ]] && exit 0
if ! ./mvnw -q -o test -Dtest=ArchitectureTest -Dsurefire.failIfNoSpecifiedTests=false \
     -Djacoco.skip=true >/tmp/arch-check.log 2>&1; then
  echo "Architecture/compile check failed. Fix before finishing (do not change ArchitectureTest):" >&2
  grep -E 'ERROR|Rule|violated|was violated|->' /tmp/arch-check.log | head -40 >&2
  exit 2
fi
exit 0

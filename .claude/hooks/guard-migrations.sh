#!/usr/bin/env bash
# PreToolUse: block changes to Flyway migrations that are already committed (fix forward instead).
set -euo pipefail
input=$(cat)
file=$(sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' <<<"$input" | head -1)
[[ -z "$file" ]] && exit 0
case "$file" in
  */src/main/resources/db/migration/*)
    cd "$CLAUDE_PROJECT_DIR"
    rel=${file#"$CLAUDE_PROJECT_DIR"/}
    if git ls-files --error-unmatch "$rel" >/dev/null 2>&1 && git cat-file -e "HEAD:$rel" 2>/dev/null; then
      echo "Blocked: $rel is a committed Flyway migration. Never modify it - add a new V<n>__*.sql migration instead (CLAUDE.md#database)." >&2
      exit 2
    fi
    ;;
esac
exit 0

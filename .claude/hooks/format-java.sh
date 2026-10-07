#!/usr/bin/env bash
# PostToolUse: format the edited Java file with Spotless (fast, single file).
set -uo pipefail
input=$(cat)
file=$(sed -n 's/.*"file_path"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' <<<"$input" | head -1)
[[ "$file" == *.java ]] || exit 0
cd "$CLAUDE_PROJECT_DIR"
regex=$(printf '%s' "$file" | sed 's/[][\.^$*+?(){}|]/\\&/g')
if ! ./mvnw -q -o spotless:apply -DspotlessFiles="$regex" >/tmp/spotless.log 2>&1; then
  # The edit already happened; exit 2 reports the formatting/syntax problem back to Claude.
  echo "Spotless failed for $file:" >&2; tail -20 /tmp/spotless.log >&2
  exit 2
fi
exit 0

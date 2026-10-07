#!/usr/bin/env bash
# One-time setup: Maven wrapper, formatting, git init, GitHub repo, branch protection.
# Usage: ./bootstrap.sh [repo-name] [--private]
# Requires: JDK 21, Maven 3.9+, Docker (for tests), gh CLI authenticated (gh auth login).
set -euo pipefail

REPO=${1:-market-pulse}
VISIBILITY=--public
[[ "${2:-}" == "--private" ]] && VISIBILITY=--private

echo "==> Maven wrapper"
mvn -q -N wrapper:wrapper -Dmaven=3.9.16

echo "==> Format sources and run full verification (first run downloads dependencies)"
./mvnw -q spotless:apply
./mvnw -B verify

echo "==> Git + GitHub"
git init -q -b main
git add -A
git commit -qm "chore: project skeleton, architecture docs, ADRs and guardrails"
gh repo create "$REPO" $VISIBILITY --source . --remote origin --push \
  --description "Reactive event-driven market data demo: Quarkus, Mutiny, Kafka, SSE"

echo "==> Branch protection on main (require PR + green CI, no force pushes)"
OWNER=$(gh api user --jq .login)
if ! gh api -X PUT "repos/$OWNER/$REPO/branches/main/protection" --input - >/dev/null <<JSON
{
  "required_status_checks": { "strict": true, "contexts": ["build"] },
  "enforce_admins": false,
  "required_pull_request_reviews": { "required_approving_review_count": 0 },
  "restrictions": null,
  "allow_force_pushes": false,
  "allow_deletions": false,
  "required_linear_history": true
}
JSON
then
  echo "!! Branch protection failed. Private repos need GitHub Pro/Team; make the repo public or set it up manually."
fi

echo "==> Done: https://github.com/$OWNER/$REPO"
echo "Next: claude  ->  /implement M0"

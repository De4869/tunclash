#!/usr/bin/env bash
# Usage: bash scripts/set-repo.sh owner/repo
# Rewrites the update channel URLs in module.prop and update.json.
set -euo pipefail
REPO="${1:?usage: set-repo.sh owner/repo}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

sed -i.bak "s#https://github.com/USER/REPO#https://github.com/${REPO}#g" "$ROOT/module/module.prop"
sed -i.bak "s#https://raw.githubusercontent.com/USER/REPO#https://raw.githubusercontent.com/${REPO}#g" "$ROOT/module/module.prop"
sed -i.bak "s#github.com/USER/REPO#github.com/${REPO}#g" "$ROOT/update.json"
rm -f "$ROOT/module/module.prop.bak" "$ROOT/update.json.bak"

echo "update channel -> $REPO"
grep updateJson "$ROOT/module/module.prop"

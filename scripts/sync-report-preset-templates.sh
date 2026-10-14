#!/usr/bin/env bash
# Sync ISO preset report templates into cat2bug-platform-admin classpath resources
# (cat2bug-platform-admin/src/main/resources/报告/模版/).
#
# Usage (from repo root):
#   ./scripts/sync-report-preset-templates.sh
#
# Copies business Markdown from readme/development/skills/report-templates/
# (29119-3, 9001/manuals, 9001/records), writes manifest.json, then renders
# readable preview PNGs via Pillow (Node/Playwright often unavailable here).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

set +e
node "$ROOT/scripts/report-preset/sync.mjs"
NODE_RC=$?
set -e

if [[ "$NODE_RC" -ne 0 ]]; then
  echo "warn: node sync exited $NODE_RC — falling back to Python rebuild" >&2
  python3 "$ROOT/scripts/report-preset/rebuild_manifest.py"
fi

python3 "$ROOT/scripts/report-preset/render_previews_pil.py"

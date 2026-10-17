#!/usr/bin/env bash
# RPM 打包已迁到 cat2bug-build。本仓库只打 JAR。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
TARGET="$(cd "$ROOT/../cat2bug-build" && pwd)/scripts/platform/build-rpm.sh"
if [[ ! -f "$TARGET" ]]; then
  echo "找不到 $TARGET" >&2
  exit 1
fi
export CAT2BUG_PLATFORM_ROOT="$ROOT"
exec "$TARGET" "$@"

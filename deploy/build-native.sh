#!/usr/bin/env bash
# Native / 多平台编译已迁到 cat2bug-build。本仓库只打 JAR：./deploy/build-embedded.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$(cd "$ROOT/../cat2bug-build" && pwd)/scripts/platform/build-linux-native.sh"
if [[ ! -f "$TARGET" ]]; then
  echo "找不到 $TARGET" >&2
  exit 1
fi
export CAT2BUG_PLATFORM_ROOT="$ROOT"
exec "$TARGET" "$@"

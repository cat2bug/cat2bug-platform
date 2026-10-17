#!/usr/bin/env bash
# UPX 压缩已迁到 cat2bug-build。
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="$(cd "$ROOT/../cat2bug-build" && pwd)/scripts/platform/upx-native-binary.sh"
if [[ ! -f "$TARGET" ]]; then
  echo "找不到 $TARGET" >&2
  exit 1
fi
export CAT2BUG_PLATFORM_ROOT="$ROOT"
exec "$TARGET" "$@"

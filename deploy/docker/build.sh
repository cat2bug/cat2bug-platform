#!/usr/bin/env bash
# Docker 前置打包（前端 dist + MySQL 版 JAR）已迁到 cat2bug-build。
# 本仓库的编译只打 JAR：./deploy/build-embedded.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TARGET="$(cd "$ROOT/../cat2bug-build" && pwd)/scripts/platform/build-docker-jar.sh"
if [[ ! -f "$TARGET" ]]; then
  echo "找不到 $TARGET" >&2
  exit 1
fi
export CAT2BUG_PLATFORM_ROOT="$ROOT"
exec "$TARGET" "$@"

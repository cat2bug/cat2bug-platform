#!/usr/bin/env bash
# 兼容入口：转发到 deploy/build-native.sh（含 embedded SPA + UPX）
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ARCH="${1:-$(uname -m)}"
exec "$ROOT/deploy/build-native.sh" "$ARCH"

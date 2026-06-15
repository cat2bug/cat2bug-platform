#!/usr/bin/env bash
# 重启后恢复双架构 Native 构建（aarch64 优先，再 amd64 + 度量）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LOG="${LOG:-/tmp/cat2bug-native-rebuild.log}"
exec > >(tee -a "$LOG") 2>&1

echo "==> $(date -Iseconds) 开始双架构 Native 重建"

build_arch() {
  local arch="$1"
  echo ""
  echo "==> $(date -Iseconds) build-native-spring.sh $arch"
  UPX_IN_DOCKER=true UPX_SMOKE=false "$ROOT/deploy/build-native-spring.sh" "$arch"
}

build_arch aarch64
build_arch x86_64

echo ""
echo "==> $(date -Iseconds) amd64 冷启动 + RPM"
"$ROOT/deploy/scripts/post-amd64-native.sh"

echo ""
echo "==> $(date -Iseconds) 全部完成；日志: $LOG"

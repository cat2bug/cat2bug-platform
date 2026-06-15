#!/usr/bin/env bash
# amd64 Native 构建完成后的本地验收：冷启动度量 + RPM 打包
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
BIN="$ROOT/cat2bug-platform-admin/target/cat2bug-admin-linux-amd64"

[[ -f "$BIN" ]] || {
  echo "[ERROR] 请先构建: UPX_IN_DOCKER=true ./deploy/build-native-spring.sh x86_64" >&2
  exit 1
}

echo "==> amd64 体积"
du -h "$BIN" "${BIN}.upx" 2>/dev/null || du -h "$BIN"

echo ""
echo "==> amd64 raw 冷启动"
ARCH=amd64 PORT=2031 "$ROOT/deploy/test/measure-native-coldstart.sh" raw 3

if [[ -f "${BIN}.upx" ]]; then
  echo ""
  echo "==> amd64 UPX 冷启动"
  ARCH=amd64 PORT=2032 "$ROOT/deploy/test/measure-native-coldstart.sh" upx 3
fi

if command -v nfpm >/dev/null 2>&1; then
  echo ""
  echo "==> amd64 RPM"
  "$ROOT/deploy/rpm/cat2bug/build-rpm-spring.sh" x86_64
  ls -lh "$ROOT/deploy/rpm/cat2bug/dist/"*x86_64*.rpm 2>/dev/null || true
else
  echo "[SKIP] nfpm 未安装，跳过 RPM（brew install nfpm）"
fi

echo ""
echo "==> 请将冷启动数据填入 readme/spring-native-delivery/METRICS.md"

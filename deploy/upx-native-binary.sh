#!/usr/bin/env bash
# 对已有 Native 二进制做 UPX 压缩（不重新编译）
#
# 用法:
#   ./deploy/upx-native-binary.sh [二进制路径]
#   UPX_IN_DOCKER=true ./deploy/upx-native-binary.sh cat2bug-platform-admin/target/cat2bug-admin-linux-arm64
#   UPX_MODE=brute UPX_IN_DOCKER=true ./deploy/upx-native-binary.sh ...
#
# Linux ELF 默认 UPX_IN_DOCKER=auto：本机无 upx 时走 Docker；macOS 上 Mach-O 仍用本机 upx + --force-macos
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=scripts/upx-args.sh
source "$ROOT/deploy/scripts/upx-args.sh"
# shellcheck source=scripts/upx-docker.sh
source "$ROOT/deploy/scripts/upx-docker.sh"

SRC="${1:?用法: $0 <native-binary>}"

if [[ ! -f "$SRC" ]]; then
  echo "[ERROR] 未找到二进制: $SRC" >&2
  exit 1
fi

BINARY_INFO="$(file -b "$SRC")"
OUT_UPX="${SRC}.upx"

use_docker=false
if [[ "$BINARY_INFO" == *"ELF"* ]]; then
  case "${UPX_IN_DOCKER:-auto}" in
    true|1|yes) use_docker=true ;;
    auto)
      if ! command -v upx >/dev/null 2>&1; then
        use_docker=true
      fi
      ;;
  esac
fi

if [[ "$use_docker" == "true" ]]; then
  upx_compress_in_docker "$SRC" "$OUT_UPX"
  exit 0
fi

if ! command -v upx >/dev/null 2>&1; then
  echo "[ERROR] 未安装 upx；Linux ELF 可设 UPX_IN_DOCKER=true" >&2
  exit 1
fi

cp -f "$SRC" "$OUT_UPX"
chmod +x "$OUT_UPX"

read -r -a UPX_ARGS <<< "$(upx_build_args)"
if [[ "$BINARY_INFO" == *"Mach-O"* ]]; then
  UPX_ARGS+=(--force-macos)
fi

echo "==> UPX (${UPX_MODE:-best}): $OUT_UPX"
upx "${UPX_ARGS[@]}" "$OUT_UPX"

RAW_BYTES="$(wc -c < "$SRC" | tr -d ' ')"
UPX_BYTES="$(wc -c < "$OUT_UPX" | tr -d ' ')"
UPX_RATIO="$(awk "BEGIN { printf \"%.2f\", ($UPX_BYTES / $RAW_BYTES) * 100 }")"
echo "raw:  $SRC ($(du -h "$SRC" | cut -f1))"
echo "upx:  $OUT_UPX ($(du -h "$OUT_UPX" | cut -f1), ${UPX_RATIO}%)"

if [[ "$BINARY_INFO" == *"Mach-O"* ]]; then
  echo "[NOTE] macOS UPX 产物不可本地执行；验证请用: ./deploy/docker/run-native-spring-minimal.sh upx <elf> && run-bg"
fi

#!/usr/bin/env bash
# 对已有 Native 二进制做 UPX 压缩（不重新编译）
#
# 用法:
#   ./deploy/upx-native-binary.sh [二进制路径]
#   ./deploy/upx-native-binary.sh cat2bug-platform-admin/target/cat2bug-admin-linux-arm64
set -euo pipefail

SRC="${1:?用法: $0 <native-binary>}"

if [[ ! -f "$SRC" ]]; then
  echo "[ERROR] 未找到二进制: $SRC" >&2
  exit 1
fi

if ! command -v upx >/dev/null 2>&1; then
  echo "[ERROR] 未安装 upx（macOS: brew install upx）" >&2
  exit 1
fi

OUT_UPX="${SRC}.upx"
cp -f "$SRC" "$OUT_UPX"
chmod +x "$OUT_UPX"

BINARY_INFO="$(file -b "$SRC")"
UPX_ARGS=(--best --lzma)
if [[ "$BINARY_INFO" == *"Mach-O"* ]]; then
  UPX_ARGS+=(--force-macos)
fi

echo "==> UPX: $OUT_UPX"
upx "${UPX_ARGS[@]}" "$OUT_UPX"

RAW_BYTES="$(wc -c < "$SRC" | tr -d ' ')"
UPX_BYTES="$(wc -c < "$OUT_UPX" | tr -d ' ')"
UPX_RATIO="$(awk "BEGIN { printf \"%.2f\", ($UPX_BYTES / $RAW_BYTES) * 100 }")"
echo "raw:  $SRC ($(du -h "$SRC" | cut -f1))"
echo "upx:  $OUT_UPX ($(du -h "$OUT_UPX" | cut -f1), ${UPX_RATIO}%)"

if [[ "$BINARY_INFO" == *"Mach-O"* ]]; then
  echo "[NOTE] macOS UPX 产物不可本地执行，仅作体积参考；发布请用 linux ELF .upx"
fi

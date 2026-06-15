#!/usr/bin/env bash
# 检查 Native ELF 是否链接 libawt（Phase 3 验收）
set -euo pipefail

BIN="${1:?用法: $0 <native-elf>}"
if [[ ! -f "$BIN" ]]; then
  echo "文件不存在: $BIN" >&2
  exit 1
fi

if file "$BIN" | grep -q "Mach-O"; then
  echo "[INFO] Mach-O 二进制，跳过 libawt.so 检查（Linux 验收以 ELF 为准）"
  exit 0
fi

if command -v ldd >/dev/null 2>&1; then
  if ldd "$BIN" 2>/dev/null | grep -qi awt; then
    echo "[FAIL] 检测到 AWT 依赖:" >&2
    ldd "$BIN" | grep -i awt >&2
    exit 1
  fi
  echo "[OK] ldd 未检出 libawt"
  exit 0
fi

# macOS 等无 ldd 时：用本机 debian 容器检查（勿指定 --platform，使用本地 arm64 镜像）
if command -v docker >/dev/null 2>&1 && docker image inspect debian:bookworm-slim >/dev/null 2>&1; then
  if docker run --rm -v "$BIN:/bin/cat2bug:ro" debian:bookworm-slim \
    bash -lc 'ldd /bin/cat2bug 2>/dev/null | grep -qi awt'; then
    echo "[FAIL] 容器 ldd 检测到 AWT 依赖" >&2
    docker run --rm -v "$BIN:/bin/cat2bug:ro" debian:bookworm-slim \
      bash -lc 'ldd /bin/cat2bug 2>/dev/null | grep -i awt' >&2
    exit 1
  fi
  echo "[OK] debian 容器 ldd 未检出 libawt"
  exit 0
fi

if command -v readelf >/dev/null 2>&1; then
  if readelf -d "$BIN" 2>/dev/null | grep -qi awt; then
    echo "[FAIL] readelf 检测到 AWT 相关 NEEDED 条目" >&2
    exit 1
  fi
  echo "[OK] readelf 未检出 AWT NEEDED"
  exit 0
fi

echo "[WARN] 无 ldd/readelf，跳过检查"
exit 0

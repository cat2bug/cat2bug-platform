#!/usr/bin/env bash
# 检查 Native ELF 是否仍包含 kaptcha 类路径痕迹
set -euo pipefail

BIN="${1:?用法: $0 <native-elf>}"
if [[ ! -f "$BIN" ]]; then
  echo "文件不存在: $BIN" >&2
  exit 1
fi

if file "$BIN" | grep -q "Mach-O"; then
  echo "[INFO] Mach-O 二进制，跳过 kaptcha 检查（Linux 验收以 ELF 为准）"
  exit 0
fi

if strings "$BIN" | rg -q 'com/google/code/kaptcha|pro/fessional/kaptcha|kaptcha-2'; then
  echo "[FAIL] 检测到 kaptcha 相关字符串:" >&2
  strings "$BIN" | rg 'com/google/code/kaptcha|pro/fessional/kaptcha|kaptcha' | head -20 >&2
  exit 1
fi

echo "[OK] strings 未检出 kaptcha"
exit 0

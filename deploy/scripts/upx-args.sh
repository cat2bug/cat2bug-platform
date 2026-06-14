#!/usr/bin/env bash
# 输出 UPX 参数数组（由调用方 eval 或 mapfile 使用）
# UPX_MODE: best（默认）| brute | ultra
upx_build_args() {
  local mode="${UPX_MODE:-best}"
  case "$mode" in
    best)
      echo --best --lzma
      ;;
    brute)
      echo --best --lzma --brute
      ;;
    ultra|ultra-brute)
      echo --best --lzma --ultra-brute
      ;;
    *)
      echo "[WARN] 未知 UPX_MODE=$mode，回退 best" >&2
      echo --best --lzma
      ;;
  esac
}

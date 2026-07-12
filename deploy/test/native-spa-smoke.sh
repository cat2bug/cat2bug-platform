#!/usr/bin/env bash
# Native SPA 静态资源冒烟（curl，不依赖 Playwright）
#
# 用法:
#   ./deploy/test/native-spa-smoke.sh
#   ./deploy/test/native-spa-smoke.sh http://127.0.0.1:2020
#
# 覆盖:
#   - GET / 返回 HTML
#   - GET /static/js/*.js（Vue2）或 /static/index-*.js（Vite）至少一个 200
#   - GET /login（Accept: text/html）SPA fallback HTML
# POST /login API 由 native-api-smoke.sh 覆盖
set -euo pipefail

BASE="${1:-http://127.0.0.1:2020}"
FAIL=0

log_ok() { echo "OK   $1"; }
log_fail() { echo "FAIL $1: $2"; FAIL=1; }

echo "==> Native SPA smoke @ $BASE"
echo

# GET /
body=$(curl -sS -D /tmp/native-spa-headers.txt "$BASE/" || true)
code=$(awk 'toupper($1) ~ /^HTTP/ { print $2; exit }' /tmp/native-spa-headers.txt 2>/dev/null || echo 000)
if [[ "$code" == "200" ]] && echo "$body" | rg -qi '<!DOCTYPE|<html'; then
  log_ok "GET / returns HTML"
else
  log_fail "GET / returns HTML" "code=$code body=${body:0:120}"
fi

# 从 index.html 解析主 bundle：Vue2 static/js/*.js 或 Vite static/*.js
js_path=""
if echo "$body" | rg -q 'static/js/[^"'\'' ]+\.js'; then
  js_path=$(echo "$body" | rg -o 'static/js/[^"'\'' ]+\.js' | head -1)
elif echo "$body" | rg -q 'static/[^"'\'' ]+\.js'; then
  js_path=$(echo "$body" | rg -o 'static/[^"'\'' ]+\.js' | head -1)
fi
if [[ -z "$js_path" ]]; then
  # 回退：探测常见 chunk 命名（Vue2 webpack / Vite embedded）
  for guess in static/js/app.js static/js/chunk-vendors.js static/index.js; do
    c=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/$guess" || echo 000)
    if [[ "$c" == "200" ]]; then
      js_path="$guess"
      break
    fi
  done
  if [[ -z "$js_path" ]]; then
    vite_guess=$(curl -sS "$BASE/index.html" 2>/dev/null | rg -o 'static/index-[^"'\'' ]+\.js' | head -1 || true)
    if [[ -n "$vite_guess" ]]; then
      c=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/$vite_guess" || echo 000)
      if [[ "$c" == "200" ]]; then
        js_path="$vite_guess"
      fi
    fi
  fi
fi
if [[ -n "$js_path" ]]; then
  js_code=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/$js_path" || echo 000)
  if [[ "$js_code" == "200" ]]; then
    log_ok "GET /$js_path"
  else
    log_fail "GET /$js_path" "code=$js_code"
  fi
else
  log_fail "GET /static/*.js" "no js path found in index.html"
fi

# SPA fallback: /login with Accept: text/html
login_body=$(curl -sS -H 'Accept: text/html' "$BASE/login" || true)
login_code=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Accept: text/html' "$BASE/login" || echo 000)
if [[ "$login_code" == "200" ]] && echo "$login_body" | rg -qi '<!DOCTYPE|<html'; then
  if echo "$login_body" | rg -q '^\s*\{'; then
    log_fail "GET /login (Accept: text/html)" "response looks like JSON"
  else
    log_ok "GET /login (Accept: text/html) SPA fallback"
  fi
else
  log_fail "GET /login (Accept: text/html)" "code=$login_code body=${login_body:0:120}"
fi

echo
echo "NOTE POST /login API covered by native-api-smoke.sh"
echo
if [[ "$FAIL" -eq 0 ]]; then
  echo "==> SPA smoke PASSED"
  exit 0
else
  echo "==> SPA smoke FAILED"
  exit 1
fi

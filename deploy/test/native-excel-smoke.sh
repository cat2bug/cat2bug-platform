#!/usr/bin/env bash
# Native Excel 导出冒烟：登录后调用关键 xlsx 接口，校验 Content-Type 与 PK 头
set -euo pipefail
BASE="${1:-http://127.0.0.1:2020}"
FAIL=0
TOKEN=""
PROJECT_ID="${PROJECT_ID:-1}"

log_ok() { echo "OK   $1"; }
log_fail() { echo "FAIL $1: $2"; FAIL=1; }

assert_xlsx() {
  local name="$1" headers="$2" body_file="$3"
  if echo "$headers" | rg -qi 'MissingReflectionRegistrationError|NoClassDefFoundError|ExceptionInInitializerError'; then
    log_fail "$name" "native runtime error in response"
    return
  fi
  if ! echo "$headers" | rg -qi 'Content-Type:.*spreadsheetml'; then
    log_fail "$name" "missing xlsx content-type: $(echo "$headers" | head -5)"
    return
  fi
  if [[ ! -s "$body_file" ]]; then
    log_fail "$name" "empty body"
    return
  fi
  if ! head -c 2 "$body_file" | xxd -p | rg -qi '504b'; then
    log_fail "$name" "not a ZIP/xlsx (missing PK header)"
    return
  fi
  log_ok "$name"
}

echo "==> Native Excel smoke @ $BASE (projectId=$PROJECT_ID)"
echo

body=$(curl -s "$BASE/captchaImage")
uuid=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("uuid",""))' 2>/dev/null || echo "")

body=$(curl -s -X POST "$BASE/login" -H 'Content-Type: application/json' \
  -d "{\"username\":\"demo\",\"password\":\"123456\",\"code\":\"\",\"uuid\":\"$uuid\"}")
TOKEN=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("token",""))' 2>/dev/null || echo "")
if [[ -z "$TOKEN" ]]; then
  echo "WARN login failed, trying admin/cat2bug"
  body=$(curl -s -X POST "$BASE/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"admin\",\"password\":\"cat2bug\",\"code\":\"\",\"uuid\":\"$uuid\"}")
  TOKEN=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("token",""))' 2>/dev/null || echo "")
fi
if [[ -z "$TOKEN" ]]; then
  log_fail "login" "no token"
  exit 1
fi
log_ok "login"

tmpdir=$(mktemp -d)
trap 'rm -rf "$tmpdir"' EXIT
AUTH=(-H "Authorization: Bearer $TOKEN")

# 缺陷导入模版
hdr=$(curl -sS -D "$tmpdir/h1.txt" -o "$tmpdir/defect-template.xlsx" -X POST \
  "${AUTH[@]}" "$BASE/system/defect/importTemplate")
assert_xlsx "POST /system/defect/importTemplate" "$(cat "$tmpdir/h1.txt")" "$tmpdir/defect-template.xlsx"

# 缺陷导出
hdr=$(curl -sS -D "$tmpdir/h2.txt" -o "$tmpdir/defect-export.xlsx" -X POST \
  "${AUTH[@]}" -F "projectId=$PROJECT_ID" "$BASE/system/defect/export")
assert_xlsx "POST /system/defect/export" "$(cat "$tmpdir/h2.txt")" "$tmpdir/defect-export.xlsx"

# 用例导入模版
hdr=$(curl -sS -D "$tmpdir/h3.txt" -o "$tmpdir/case-template.xlsx" -X POST \
  "${AUTH[@]}" -F "projectId=$PROJECT_ID" "$BASE/system/case/importTemplate")
assert_xlsx "POST /system/case/importTemplate" "$(cat "$tmpdir/h3.txt")" "$tmpdir/case-template.xlsx"

# 用户导入模版
hdr=$(curl -sS -D "$tmpdir/h4.txt" -o "$tmpdir/user-template.xlsx" -X POST \
  "${AUTH[@]}" "$BASE/system/user/importTemplate")
assert_xlsx "POST /system/user/importTemplate" "$(cat "$tmpdir/h4.txt")" "$tmpdir/user-template.xlsx"

# 仪表盘走势（Native 方案 A：仅数据表）
hdr=$(curl -sS -D "$tmpdir/h5.txt" -o "$tmpdir/defect-line.xlsx" -X POST \
  "${AUTH[@]}" "$BASE/system/dashboard/$PROJECT_ID/defect-line/export?timeType=day")
assert_xlsx "POST /system/dashboard/{id}/defect-line/export" "$(cat "$tmpdir/h5.txt")" "$tmpdir/defect-line.xlsx"

echo
if [[ "$FAIL" -eq 0 ]]; then
  echo "Native Excel smoke passed."
  exit 0
fi
echo "Native Excel smoke FAILED."
exit 1

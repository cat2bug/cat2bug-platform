#!/usr/bin/env bash
# Native API 冒烟（L1–L4），兼容 Node 16 环境，依赖 curl
set -euo pipefail
BASE="${1:-http://127.0.0.1:2020}"
FAIL=0
TOKEN=""

log_ok() { echo "OK   $1"; }
log_fail() { echo "FAIL $1: $2"; FAIL=1; }

assert_no_native_err() {
  local name="$1" body="$2" code="$3"
  if echo "$body" | rg -q 'MissingReflectionRegistrationError|NoClassDefFoundError|ExceptionInInitializerError|Could not initialize class|Executor\.query|SSLMSW'; then
    log_fail "$name" "$body"
    return
  fi
  if [[ "$code" == "200" ]]; then
    log_ok "$name"
  else
    log_fail "$name" "code=$code body=${body:0:120}"
  fi
}

echo "==> Native API smoke @ $BASE"
echo

body=$(curl -s "$BASE/version")
code=$(echo "$body" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("code",200))' 2>/dev/null || echo 200)
assert_no_native_err "L1 GET /version" "$body" "$code"

body=$(curl -s "$BASE/captchaImage")
code=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))')
assert_no_native_err "L1 GET /captchaImage" "$body" "$code"

body=$(curl -s "$BASE/setup/status")
code=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))')
assert_no_native_err "L1 GET /setup/status" "$body" "$code"

body=$(curl -s -X POST "$BASE/login" -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"123456","code":"","uuid":""}')
TOKEN=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("token",""))' 2>/dev/null || echo "")
if [[ -z "$TOKEN" ]]; then
  echo "WARN demo login failed, trying admin/cat2bug"
  body=$(curl -s -X POST "$BASE/login" -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"cat2bug","code":"","uuid":""}')
  TOKEN=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("token",""))' 2>/dev/null || echo "")
fi
code=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))')
assert_no_native_err "L2 POST /login" "$body" "$code"

if [[ -z "$TOKEN" ]]; then
  echo "No token, abort"
  exit 1
fi

body=$(curl -s "$BASE/getInfo" -H "Authorization: Bearer $TOKEN")
code=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))')
assert_no_native_err "L2 GET /getInfo" "$body" "$code"

echo 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==' | base64 -d > /tmp/native-smoke.png
body=$(curl -s -X POST "$BASE/common/upload" -H "Authorization: Bearer $TOKEN" -F 'file=@/tmp/native-smoke.png')
code=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))')
assert_no_native_err "L3 POST /common/upload" "$body" "$code"
ICON=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("url") or json.load(open("/dev/stdin"))' 2>/dev/null || true)
ICON=$(echo "$body" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("url") or d.get("fileName") or "/profile/upload/smoke.png")')

body=$(curl -s -X POST "$BASE/system/team" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"teamName\":\"smoke-$(date +%s)\",\"teamIcon\":\"$ICON\",\"remark\":\"native smoke\"}")
code=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))')
assert_no_native_err "L3 POST /system/team (create)" "$body" "$code"

body=$(curl -s "$BASE/system/team/my" -H "Authorization: Bearer $TOKEN")
code=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))')
assert_no_native_err "L3 GET /system/team/my" "$body" "$code"

for path in \
  "/system/team/list?pageNum=1&pageSize=10" \
  "/system/project/list?pageNum=1&pageSize=10" \
  "/system/defect/list?pageNum=1&pageSize=10" \
  "/system/case/list?pageNum=1&pageSize=10" \
  "/system/user/list?pageNum=1&pageSize=10" \
  "/admin/team/list?pageNum=1&pageSize=10"; do
  body=$(curl -s "$BASE$path" -H "Authorization: Bearer $TOKEN")
  code=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))')
  if echo "$body" | rg -q 'MissingReflectionRegistrationError|NoClassDefFoundError|ExceptionInInitializerError|Could not initialize class|Executor\.query|SSLMSW'; then
    log_fail "L4 GET $path" "$body"
  else
    log_ok "L4 GET $path"
  fi
done

echo
if [[ "$FAIL" -eq 0 ]]; then
  echo "==> ALL PASSED"
  exit 0
else
  echo "==> SOME FAILED"
  exit 1
fi

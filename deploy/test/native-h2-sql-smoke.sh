#!/usr/bin/env bash
# Native Docker Phase 2 抽样 + H2 SQL 分支冒烟（公共接口、仪表盘、缺陷统计）
# 兼容 Node 16 环境：bash + curl + python3（无 fetch）
set -euo pipefail
BASE="${1:-http://127.0.0.1:2020}"
FAIL=0

log_ok() { echo "OK   $1"; }
log_fail() { echo "FAIL $1: $2"; FAIL=1; }

assert_h2_ok() {
  local name="$1" body="$2"
  if echo "$body" | rg -qi 'MissingReflectionRegistrationError|NoClassDefFoundError|ExceptionInInitializerError|Could not initialize class|json_contains|date_format|Function "|JdbcSQLSyntaxErrorException|Executor\.query|SSLMSW'; then
    log_fail "$name" "$(echo "$body" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(str(d.get("msg",""))[:160])' 2>/dev/null || echo "$body" | head -c 160)"
    return
  fi
  local code
  code=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))' 2>/dev/null || echo 500)
  if [[ "$code" == "200" ]]; then
    log_ok "$name"
  else
    log_fail "$name" "code=$code $(echo "$body" | python3 -c 'import sys,json; print(str(json.load(sys.stdin).get("msg",""))[:120])' 2>/dev/null)"
  fi
}

echo "==> Native Phase 2 + H2 SQL smoke @ $BASE"
echo

# Phase 2 抽样：无鉴权公共接口
for path in "/captchaImage" "/setup/status"; do
  body=$(curl -s "$BASE$path")
  assert_h2_ok "GET $path" "$body"
done

TOKEN=$(curl -s -X POST "$BASE/login" -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"123456","code":"","uuid":""}' \
  | python3 -c 'import sys,json; print(json.load(sys.stdin).get("token",""))')
[[ -n "$TOKEN" ]] || { echo "login failed"; exit 1; }

PID=$(curl -s "$BASE/system/project/list?pageNum=1&pageSize=1" -H "Authorization: Bearer $TOKEN" \
  | python3 -c 'import sys,json; r=json.load(sys.stdin); print((r.get("rows") or [{}])[0].get("projectId",1))')

paths=(
  "/system/defect/statistic/type/${PID}"
  "/system/dashboard/${PID}/defect"
  "/system/defect/statistic/open-workload/${PID}?pageNum=1&pageSize=10"
  "/system/defect/statistic/open-workload/${PID}/my"
  "/system/dashboard/${PID}/defect-line?timeType=day"
  "/system/dashboard/${PID}/member-defect-line?timeType=day"
  "/system/dashboard/${PID}/member-defect"
  "/system/defect/statistic/state/${PID}"
)

for path in "${paths[@]}"; do
  body=$(curl -s "$BASE$path" -H "Authorization: Bearer $TOKEN")
  assert_h2_ok "GET $path" "$body"
done

echo
if [[ "$FAIL" -eq 0 ]]; then
  echo "==> H2 SQL ALL PASSED"
else
  echo "==> H2 SQL SOME FAILED"
  exit 1
fi

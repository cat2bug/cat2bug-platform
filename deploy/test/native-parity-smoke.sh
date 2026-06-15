#!/usr/bin/env bash
# Native Phase 2 扩展冒烟：Open API / WebSocket / AI / Quartz / Setup / Upgrade
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE="${1:-http://127.0.0.1:2020}"
FAIL=0
TOKEN=""
PROJECT_ID=""
API_KEY=""

log_ok() { echo "OK   $1"; }
log_fail() { echo "FAIL $1: $2"; FAIL=1; }

assert_no_native_err() {
  local name="$1" body="$2" expect_code="${3:-200}"
  if echo "$body" | rg -q 'MissingReflectionRegistrationError|NoClassDefFoundError|ExceptionInInitializerError|Could not initialize class'; then
    log_fail "$name" "native runtime error"
    return
  fi
  local code
  code=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))' 2>/dev/null || echo 500)
  if [[ "$code" == "$expect_code" ]]; then
    log_ok "$name"
  else
    log_fail "$name" "code=$code $(echo "$body" | python3 -c 'import sys,json; print(str(json.load(sys.stdin).get("msg",""))[:120])' 2>/dev/null || echo "$body" | head -c 120)"
  fi
}

ws_host_port_path() {
  local u="${BASE#http://}"
  u="${u#https://}"
  local host port
  if [[ "$u" == */* ]]; then
    host="${u%%/*}"
  else
    host="$u"
  fi
  if [[ "$host" == *:* ]]; then
    port="${host##*:}"
    host="${host%%:*}"
  else
    port=80
  fi
  echo "$host" "$port"
}

echo "==> Native Phase 2 parity smoke @ $BASE"
echo

# --- 2.8 Setup / 2.9 Upgrade（无鉴权）---
body=$(curl -s "$BASE/setup/status")
assert_no_native_err "2.8 GET /setup/status" "$body"
INSTALLED=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("installed",False))' 2>/dev/null || echo false)
if [[ "$INSTALLED" != "True" && "$INSTALLED" != "true" ]]; then
  log_fail "2.8 setup installed" "installed=$INSTALLED (需先完成 /setup/submit)"
fi

body=$(curl -s "$BASE/upgrade/status")
assert_no_native_err "2.9 GET /upgrade/status" "$body"
UPGRADE_REQ=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("upgradeRequired",True))' 2>/dev/null || echo true)
STATE=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("data",{}).get("state",""))' 2>/dev/null || echo "")
if [[ "$UPGRADE_REQ" == "True" || "$UPGRADE_REQ" == "true" ]]; then
  log_fail "2.9 upgrade not required" "upgradeRequired=true state=$STATE"
else
  log_ok "2.9 upgrade not required (state=$STATE)"
fi

# --- bootstrap（Open API / AI / Quartz 需登录）---
eval "$("$SCRIPT_DIR/native-smoke-bootstrap.sh" "$BASE")"
if [[ -z "$PROJECT_ID" ]]; then
  log_fail "bootstrap" "no project id"
  exit 1
fi
AUTH=(-H "Authorization: Bearer $TOKEN")

# --- 2.4 Open API ---
API_NAME="native-smoke-api-$(date +%s)"
api_body=$(curl -s -X POST "${AUTH[@]}" -H 'Content-Type: application/json' "$BASE/system/api" \
  -d "{\"projectId\":$PROJECT_ID,\"apiName\":\"$API_NAME\",\"enabled\":true}")
api_code=$(echo "$api_body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))' 2>/dev/null || echo 500)
if [[ "$api_code" != "200" ]]; then
  log_fail "2.4 POST /system/api (create key)" "$(echo "$api_body" | head -c 160)"
else
  log_ok "2.4 POST /system/api (create key)"
  list_body=$(curl -s "${AUTH[@]}" "$BASE/system/api/list?projectId=$PROJECT_ID&pageNum=1&pageSize=50")
  API_KEY=$(echo "$list_body" | python3 -c "
import sys,json
name=sys.argv[1]
d=json.load(sys.stdin)
for r in d.get('rows') or []:
    if r.get('apiName')==name:
        print(r.get('apiId',''))
        break
" "$API_NAME" 2>/dev/null || echo "")
fi

if [[ -n "$API_KEY" ]]; then
  hdr=(-H "CAT2BUG-API-KEY: $API_KEY")
  body=$(curl -s "${hdr[@]}" "$BASE/api/project")
  assert_no_native_err "2.4 GET /api/project" "$body"
  body=$(curl -s "${hdr[@]}" "$BASE/api/case?pageNum=1&pageSize=5")
  assert_no_native_err "2.4 GET /api/case" "$body"
else
  log_fail "2.4 Open API calls" "no API_KEY from list"
fi

# --- 2.5 WebSocket ---
read -r WS_HOST WS_PORT < <(ws_host_port_path)
WS_PATH="/websocket/1/message"
if python3 "$SCRIPT_DIR/native-websocket-smoke.py" "$WS_HOST" "$WS_PORT" "$WS_PATH"; then
  log_ok "2.5 WebSocket connect $WS_PATH"
else
  log_fail "2.5 WebSocket connect" "$WS_PATH"
fi

# --- 2.6 AI（配置读取，无需 Ollama 在线）---
body=$(curl -s "${AUTH[@]}" "$BASE/system/ai/project-model-options?projectId=$PROJECT_ID")
assert_no_native_err "2.6 GET /system/ai/project-model-options" "$body"
body=$(curl -s "${AUTH[@]}" "$BASE/system/ai/list?pageNum=1&pageSize=5&projectId=$PROJECT_ID")
# Ollama 未配置时可能返回业务错误，但不应是 Native 反射错误
if echo "$body" | rg -q 'MissingReflectionRegistrationError|NoClassDefFoundError|ExceptionInInitializerError'; then
  log_fail "2.6 GET /system/ai/list" "native runtime error"
else
  log_ok "2.6 GET /system/ai/list (no native crash)"
fi

# --- 2.7 Quartz（启动日志无 Scheduler 致命错误）---
CONTAINER="${NATIVE_SMOKE_CONTAINER:-cat2bug-spring-native-minimal}"
if docker ps --format '{{.Names}}' | rg -qx "$CONTAINER"; then
  if docker logs "$CONTAINER" 2>&1 | rg -qi 'SchedulerException|QuartzScheduler.*[Ff]ailed|Could not initialize.*[Qq]uartz'; then
    log_fail "2.7 Quartz startup logs" "fatal scheduler error found"
  else
    log_ok "2.7 Quartz scheduler (no fatal init errors in container logs)"
  fi
else
  log_fail "2.7 Quartz" "container $CONTAINER not running (set NATIVE_SMOKE_CONTAINER)"
fi

echo
if [[ "$FAIL" -eq 0 ]]; then
  echo "Native Phase 2 parity smoke passed."
  exit 0
fi
echo "Native Phase 2 parity smoke FAILED."
exit 1

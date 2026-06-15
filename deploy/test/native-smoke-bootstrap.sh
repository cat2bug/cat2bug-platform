#!/usr/bin/env bash
# 为 fresh setup / admin 账号补齐团队、项目与 userConfig（currentTeamId + currentProjectId）
# 用法：
#   eval "$(./deploy/test/native-smoke-bootstrap.sh http://127.0.0.1:2025)"
#   echo "TOKEN=$TOKEN PROJECT_ID=$PROJECT_ID"
set -euo pipefail

BASE="${1:-http://127.0.0.1:2020}"
TOKEN=""
TEAM_ID=""
PROJECT_ID=""
USER_CONFIG_ID=""
USER_ID=""

die() { echo "native-smoke-bootstrap: $*" >&2; exit 1; }

login() {
  local body uuid token
  body=$(curl -s "$BASE/captchaImage")
  uuid=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("uuid",""))' 2>/dev/null || echo "")
  body=$(curl -s -X POST "$BASE/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"demo\",\"password\":\"123456\",\"code\":\"\",\"uuid\":\"$uuid\"}")
  token=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("token",""))' 2>/dev/null || echo "")
  if [[ -z "$token" ]]; then
    body=$(curl -s -X POST "$BASE/login" -H 'Content-Type: application/json' \
      -d "{\"username\":\"admin\",\"password\":\"cat2bug\",\"code\":\"\",\"uuid\":\"$uuid\"}")
    token=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("token",""))' 2>/dev/null || echo "")
  fi
  [[ -n "$token" ]] || die "login failed (demo and admin)"
  TOKEN="$token"
}

upload_icon() {
  local png body url
  png=$(mktemp /tmp/native-smoke-XXXX.png)
  echo 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==' | base64 -d > "$png"
  body=$(curl -s -X POST "$BASE/common/upload" -H "Authorization: Bearer $TOKEN" -F "file=@$png")
  rm -f "$png"
  url=$(echo "$body" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("url") or d.get("fileName") or "")' 2>/dev/null || echo "")
  [[ -n "$url" ]] || url="/profile/upload/smoke.png"
  echo "$url"
}

load_context() {
  local body
  body=$(curl -s "$BASE/getInfo" -H "Authorization: Bearer $TOKEN")
  eval "$(echo "$body" | python3 -c '
import json, sys, shlex
d = json.load(sys.stdin)
cfg = d.get("config") or {}
user = d.get("user") or {}
pairs = [
    ("USER_CONFIG_ID", cfg.get("userConfigId")),
    ("TEAM_ID", cfg.get("currentTeamId")),
    ("PROJECT_ID", cfg.get("currentProjectId")),
    ("USER_ID", user.get("userId")),
]
for k, v in pairs:
    val = "" if v is None else str(v)
    print("export {}={}".format(k, shlex.quote(val)))
')"
}

ensure_team() {
  local body icon team_id
  if [[ -n "$TEAM_ID" && "$TEAM_ID" != "None" ]]; then
    return
  fi
  icon=$(upload_icon)
  body=$(curl -s -X POST "$BASE/system/team" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d "{\"teamName\":\"smoke-$(date +%s)\",\"teamIcon\":\"$icon\",\"remark\":\"native smoke bootstrap\"}")
  team_id=$(echo "$body" | python3 -c 'import sys,json; d=json.load(sys.stdin); print((d.get("data") or {}).get("teamId") or "")' 2>/dev/null || echo "")
  [[ -n "$team_id" ]] || die "create team failed: ${body:0:200}"
  TEAM_ID="$team_id"
  if [[ -n "$USER_CONFIG_ID" && "$USER_CONFIG_ID" != "None" ]]; then
    curl -s -X PUT "$BASE/system/user-config" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
      -d "{\"userConfigId\":$USER_CONFIG_ID,\"userId\":$USER_ID,\"currentTeamId\":$TEAM_ID}" >/dev/null
  fi
}

ensure_project() {
  local body project_id code
  if [[ -n "$PROJECT_ID" && "$PROJECT_ID" != "None" ]]; then
    return
  fi
  [[ -n "$TEAM_ID" && "$TEAM_ID" != "None" ]] || die "team id missing before project create"
  body=$(curl -s -X POST "$BASE/system/project" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
    -d "{\"teamId\":$TEAM_ID,\"projectName\":\"smoke-excel-$(date +%s)\",\"projectIntroduce\":\"native excel smoke\",\"members\":[]}")
  code=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))' 2>/dev/null || echo 500)
  [[ "$code" == "200" ]] || die "create project failed: ${body:0:200}"
  body=$(curl -s "$BASE/system/project/list?teamId=$TEAM_ID&pageNum=1&pageSize=1" -H "Authorization: Bearer $TOKEN")
  project_id=$(echo "$body" | python3 -c 'import sys,json; rows=json.load(sys.stdin).get("rows") or []; print(rows[0]["projectId"] if rows else "")' 2>/dev/null || echo "")
  [[ -n "$project_id" ]] || die "project list empty after create"
  PROJECT_ID="$project_id"
  if [[ -n "$USER_CONFIG_ID" && "$USER_CONFIG_ID" != "None" ]]; then
    curl -s -X PUT "$BASE/system/user-config" -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
      -d "{\"userConfigId\":$USER_CONFIG_ID,\"userId\":$USER_ID,\"currentTeamId\":$TEAM_ID,\"currentProjectId\":$PROJECT_ID}" >/dev/null
  fi
}

login
load_context
ensure_team
ensure_project

printf 'export TOKEN=%q\n' "$TOKEN"
printf 'export TEAM_ID=%q\n' "$TEAM_ID"
printf 'export PROJECT_ID=%q\n' "$PROJECT_ID"
printf 'export USER_CONFIG_ID=%q\n' "$USER_CONFIG_ID"

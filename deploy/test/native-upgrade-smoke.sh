#!/usr/bin/env bash
# Native Legacy Upgrade 全流程冒烟（L3：completed install + Flyway 待执行）
#
# 隔离卷与端口，不污染 2025/2027 常规冒烟环境；不设置 CAT2BUG_UPGRADE_SKIP。
#
# 用法:
#   ./deploy/test/native-upgrade-smoke.sh
#   PORT=2028 BIN=cat2bug-platform-admin/target/cat2bug-admin-linux-arm64 ./deploy/test/native-upgrade-smoke.sh
#
# 环境变量:
#   PORT / CONTAINER / DATA_VOL / CONFIG_VOL
#   SOURCE_DATA_VOL / SOURCE_CONFIG_VOL  种子数据（默认 cat2bug-spring-native-*）
#   KEEP_CONTAINER=1  结束后保留容器
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
# shellcheck source=../scripts/h2-exec.sh
source "$ROOT/deploy/scripts/h2-exec.sh"

PORT="${PORT:-2028}"
CONTAINER="${CONTAINER:-cat2bug-native-upgrade-smoke}"
DATA_VOL="${DATA_VOL:-cat2bug-native-upgrade-smoke-data}"
CONFIG_VOL="${CONFIG_VOL:-cat2bug-native-upgrade-smoke-config}"
UPLOAD_VOL="${UPLOAD_VOL:-cat2bug-native-upgrade-smoke-upload}"
SOURCE_DATA_VOL="${SOURCE_DATA_VOL:-cat2bug-spring-native-data}"
SOURCE_CONFIG_VOL="${SOURCE_CONFIG_VOL:-cat2bug-spring-native-config}"
SOURCE_UPLOAD_VOL="${SOURCE_UPLOAD_VOL:-cat2bug-spring-native-upload}"
BIN="${BIN:-$ROOT/cat2bug-platform-admin/target/cat2bug-admin-linux-arm64}"
BASE="http://127.0.0.1:${PORT}"
SEED_IMAGE="${SEED_IMAGE:-debian:bookworm-slim}"
FAIL=0

log_ok() { echo "OK   $1"; }
log_fail() { echo "FAIL $1: $2"; FAIL=1; }
log_step() { echo ""; echo "==> $1"; }

json_field() {
  local body="$1" expr="$2"
  echo "$body" | python3 -c "import sys,json; d=json.load(sys.stdin); $expr" 2>/dev/null || echo ""
}

wait_http() {
  local path="$1" tries="${2:-180}"
  for _ in $(seq 1 "$tries"); do
    if curl -sf "${BASE}${path}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 0.5
  done
  return 1
}

ensure_volume_seeded() {
  local dest="$1" src="$2"
  docker volume create "$dest" >/dev/null
  if docker run --rm -v "${dest}:/dest" "$SEED_IMAGE" sh -c 'ls -A /dest 2>/dev/null | grep -q .'; then
    return 0
  fi
  if ! docker volume inspect "$src" >/dev/null 2>&1; then
    echo "[ERROR] 种子卷不存在: $src（请先完成 setup 冒烟）" >&2
    exit 1
  fi
  echo "    从 $src 复制到 $dest"
  docker run --rm \
    -v "${src}:/from:ro" \
    -v "${dest}:/to" \
    "$SEED_IMAGE" sh -c 'cp -a /from/. /to/'
}

stop_container() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
}

start_container() {
  [[ -f "$BIN" ]] || {
    echo "[ERROR] Native 二进制不存在: $BIN" >&2
    exit 1
  }
  stop_container
  docker run -d --restart unless-stopped --name "$CONTAINER" \
    --platform "${DOCKER_PLATFORM:-linux/arm64}" \
    -w /app \
    -p "${PORT}:2020" \
    -v "${DATA_VOL}:/app/data" \
    -v "${UPLOAD_VOL}:/app/upload" \
    -v "${CONFIG_VOL}:/app/config/install" \
    -v "${BIN}:/cat2bug-admin:ro" \
    debian:bookworm-slim \
    /cat2bug-admin --server.port=2020
}

inject_schema_drift() {
  local max_ver out
  out=$(h2_exec_sql "$DATA_VOL" \
    "SELECT version FROM sys_db_version ORDER BY installed_rank DESC LIMIT 1;" 2>/dev/null || true)
  max_ver=$(echo "$out" | sed -n '2p' | tr -d '[:space:]')
  [[ -n "$max_ver" && "$max_ver" != "version" ]] || {
    echo "[ERROR] 无法读取 sys_db_version 最高版本" >&2
    echo "$out" >&2
    exit 1
  }
  echo "    当前最高版本: $max_ver"
  h2_exec_sql "$DATA_VOL" "DELETE FROM sys_db_version WHERE version = '${max_ver}';" >/dev/null
  echo "    已删除版本行: $max_ver"
}

assert_upgrade_pending() {
  local body req state pending skipped
  body=$(curl -s "${BASE}/upgrade/status")
  req=$(json_field "$body" "print(d.get('data',{}).get('upgradeRequired',False))")
  state=$(json_field "$body" "print(d.get('data',{}).get('state',''))")
  pending=$(json_field "$body" "print(len(d.get('data',{}).get('pendingMigrations') or []))")
  skipped=$(json_field "$body" "print(d.get('data',{}).get('skipped',False))")
  if [[ "$skipped" == "True" || "$skipped" == "true" ]]; then
    log_fail "upgrade skip disabled" "skipped=true（容器勿设 CAT2BUG_UPGRADE_SKIP）"
    return
  fi
  if [[ "$req" == "True" || "$req" == "true" ]] && [[ "$pending" -gt 0 ]]; then
    log_ok "upgrade pending (state=$state, pending=$pending)"
  else
    log_fail "upgrade pending" "upgradeRequired=$req state=$state pending=$pending body=${body:0:200}"
  fi
}

assert_login_blocked() {
  local body code
  body=$(curl -s -X POST "${BASE}/login" -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"cat2bug","code":"","uuid":""}')
  code=$(json_field "$body" "print(d.get('code',500))")
  if [[ "$code" != "200" ]]; then
    log_ok "login blocked during upgrade (code=$code)"
  else
    log_fail "login blocked" "got token during upgrade"
  fi
}

assert_version_ok() {
  if curl -sf "${BASE}/version" >/dev/null; then
    log_ok "GET /version during upgrade lock"
  else
    log_fail "GET /version" "unreachable"
  fi
}

submit_upgrade() {
  local body code msg
  body=$(curl -s -X POST "${BASE}/upgrade/submit" -H 'Content-Type: application/json' -d '{
    "databaseType": "h2",
    "databaseMode": "existing",
    "cacheType": "local",
    "adminUsername": "admin",
    "adminPassword": "cat2bug",
    "backupEnabled": true,
    "applyConfigChanges": false
  }')
  code=$(json_field "$body" "print(d.get('code',500))")
  msg=$(json_field "$body" "print(str(d.get('msg',''))[:120])")
  if [[ "$code" == "200" ]]; then
    log_ok "POST /upgrade/submit"
  else
    log_fail "POST /upgrade/submit" "code=$code msg=$msg"
    echo "$body" | python3 -m json.tool 2>/dev/null || echo "$body"
  fi
}

wait_post_submit_restart() {
  # submit 后 JVM 约 2s 退出；unless-stopped 由 Docker 拉起
  sleep 3
  if ! wait_http "/version" 240; then
    log_fail "restart after submit" "/version timeout"
    return
  fi
  log_ok "container restarted after submit"
}

assert_upgrade_completed() {
  local body req state
  body=$(curl -s "${BASE}/upgrade/status")
  req=$(json_field "$body" "print(d.get('data',{}).get('upgradeRequired',True))")
  state=$(json_field "$body" "print(d.get('data',{}).get('state',''))")
  if [[ ("$req" == "False" || "$req" == "false") && "$state" == "completed" ]]; then
    log_ok "upgrade completed (state=$state)"
  else
    log_fail "upgrade completed" "upgradeRequired=$req state=$state body=${body:0:200}"
  fi
}

assert_login_ok() {
  local body token
  body=$(curl -s -X POST "${BASE}/login" -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"cat2bug","code":"","uuid":""}')
  token=$(json_field "$body" "print(d.get('token',''))")
  if [[ -n "$token" ]]; then
    log_ok "login after upgrade"
  else
    log_fail "login after upgrade" "$(json_field "$body" "print(str(d.get('msg',''))[:120])")"
  fi
}

cleanup() {
  if [[ "${KEEP_CONTAINER:-}" == "1" ]]; then
    echo "保留容器 $CONTAINER（KEEP_CONTAINER=1）"
    return
  fi
  stop_container
}

trap cleanup EXIT

log_step "Native Legacy Upgrade smoke @ $BASE"
log_step "准备隔离卷"
ensure_volume_seeded "$DATA_VOL" "$SOURCE_DATA_VOL"
ensure_volume_seeded "$CONFIG_VOL" "$SOURCE_CONFIG_VOL"
ensure_volume_seeded "$UPLOAD_VOL" "$SOURCE_UPLOAD_VOL"

log_step "注入 Flyway drift（删除 sys_db_version 最高版本）"
stop_container
inject_schema_drift

log_step "启动 Native 容器（无 CAT2BUG_UPGRADE_SKIP）"
start_container
if ! wait_http "/upgrade/status"; then
  echo "[ERROR] 服务启动超时" >&2
  docker logs "$CONTAINER" 2>&1 | tail -40
  exit 1
fi

log_step "2.9.1 pending 检测"
assert_upgrade_pending

log_step "2.9.2 升级全锁"
assert_version_ok
assert_login_blocked

log_step "2.9.3 preflight"
body=$(curl -s "${BASE}/upgrade/preflight")
code=$(json_field "$body" "print(d.get('code',500))")
if [[ "$code" == "200" ]]; then
  log_ok "GET /upgrade/preflight"
else
  log_fail "GET /upgrade/preflight" "code=$code"
fi

log_step "2.9.4 submit → restart"
submit_upgrade
wait_post_submit_restart

log_step "2.9.5 重启后 completed + 登录恢复"
assert_upgrade_completed
assert_login_ok

echo ""
if [[ "$FAIL" -eq 0 ]]; then
  echo "==> native-upgrade-smoke PASSED"
  exit 0
fi
echo "==> native-upgrade-smoke FAILED"
docker logs "$CONTAINER" 2>&1 | tail -30 || true
exit 1

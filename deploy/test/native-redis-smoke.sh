#!/usr/bin/env bash
# Redis 连通与 Setup 测试接口冒烟（JVM / Native 均可；验证生产 Redis 配置路径）
#
# 用法:
#   ./deploy/test/native-redis-smoke.sh http://127.0.0.1:2020
#   ./deploy/test/native-redis-smoke.sh --with-redis http://127.0.0.1:2020
#
# --with-redis: 自动 docker run redis:7-alpine :6379，结束后 docker rm -f
set -euo pipefail

BASE=""
WITH_REDIS=0
REDIS_CONTAINER="${REDIS_CONTAINER:-cat2bug-redis-smoke}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
FAIL=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --with-redis) WITH_REDIS=1; shift ;;
    http://*|https://*) BASE="$1"; shift ;;
    *) echo "Unknown arg: $1" >&2; exit 1 ;;
  esac
done
BASE="${BASE:-http://127.0.0.1:2020}"

log_ok() { echo "OK   $1"; }
log_fail() { echo "FAIL $1: $2"; FAIL=1; }

cleanup_redis() {
  docker rm -f "$REDIS_CONTAINER" >/dev/null 2>&1 || true
}

if [[ "$WITH_REDIS" -eq 1 ]]; then
  cleanup_redis
  docker run -d --name "$REDIS_CONTAINER" -p "${REDIS_PORT}:6379" redis:7-alpine >/dev/null
  trap cleanup_redis EXIT
  for _ in $(seq 1 30); do
    if docker exec "$REDIS_CONTAINER" redis-cli ping 2>/dev/null | rg -q PONG; then
      break
    fi
    sleep 0.2
  done
fi

echo "==> Native Redis smoke @ $BASE (redis=$REDIS_HOST:$REDIS_PORT)"
echo

if docker ps --format '{{.Names}}' 2>/dev/null | rg -q "^${REDIS_CONTAINER}$"; then
  ping=$(docker exec "$REDIS_CONTAINER" redis-cli ping 2>/dev/null || echo FAIL)
  if [[ "$ping" == "PONG" ]]; then
    log_ok "redis-cli PING"
  else
    log_fail "redis-cli PING" "$ping"
  fi
else
  if command -v redis-cli >/dev/null 2>&1; then
    ping=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping 2>/dev/null || echo FAIL)
    if [[ "$ping" == "PONG" ]]; then
      log_ok "redis-cli PING (host)"
    else
      log_fail "redis-cli PING (host)" "$ping — use --with-redis or start Redis"
    fi
  else
    echo "SKIP redis-cli (no local redis-cli; use --with-redis)"
  fi
fi

body=$(curl -sS -X POST "$BASE/setup/test/redis" -H 'Content-Type: application/json' \
  -d "{\"host\":\"$REDIS_HOST\",\"port\":$REDIS_PORT,\"password\":\"\",\"database\":0}" || true)
code=$(echo "$body" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("code",500))' 2>/dev/null || echo 500)
if [[ "$code" == "200" ]]; then
  log_ok "POST /setup/test/redis"
else
  log_fail "POST /setup/test/redis" "code=$code body=${body:0:160}"
fi

u1=$(curl -sS "$BASE/captchaImage" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("uuid",""))' 2>/dev/null || echo "")
u2=$(curl -sS "$BASE/captchaImage" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("uuid",""))' 2>/dev/null || echo "")
if [[ -n "$u1" && -n "$u2" && "$u1" != "$u2" ]]; then
  log_ok "GET /captchaImage x2 (distinct uuid)"
elif [[ -n "$u1" ]]; then
  log_ok "GET /captchaImage (uuid=$u1)"
else
  log_fail "GET /captchaImage" "empty uuid"
fi

echo
echo "NOTE Native 多实例共享缓存限制见 readme/spring-native-delivery/J2CACHE-NATIVE.md"
echo
if [[ "$FAIL" -eq 0 ]]; then
  echo "==> Redis smoke PASSED"
  exit 0
else
  echo "==> Redis smoke FAILED"
  exit 1
fi

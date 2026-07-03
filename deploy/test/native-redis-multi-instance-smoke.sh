#!/usr/bin/env bash
# Native 双实例 + 共享 Redis 冒烟（手动步骤 + 可选半自动探测）
#
# J2Cache Redis key 格式: {region}:{key}，例如 verifyCode:{uuid}、tokenExpireTime:{token}
#
# 前置:
#   1. Redis 已启动且与两个 Native 实例同网可达
#   2. 两个实例均已通过安装向导完成 setup，且 cat2bug.cache.type=redis
#   3. spring.redis.host 指向同一 Redis（compose 网络内通常为 redis 服务名）
#
# 用法:
#   ./deploy/test/native-redis-multi-instance-smoke.sh
#   BASE_A=http://127.0.0.1:2020 BASE_B=http://127.0.0.1:2021 \
#     REDIS_HOST=127.0.0.1 ./deploy/test/native-redis-multi-instance-smoke.sh --probe
#
# --probe: 仅探测 Redis 中是否已有 J2Cache key（不启动实例）
set -euo pipefail

BASE_A="${BASE_A:-http://127.0.0.1:2020}"
BASE_B="${BASE_B:-http://127.0.0.1:2021}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_CONTAINER="${REDIS_CONTAINER:-cat2bug-redis-smoke}"
PROBE=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --probe) PROBE=1; shift ;;
    http://*|https://*)
      if [[ "$BASE_A" == "http://127.0.0.1:2020" ]]; then
        BASE_A="$1"
      else
        BASE_B="$1"
      fi
      shift
      ;;
    *) echo "Unknown arg: $1" >&2; exit 1 ;;
  esac
done

redis_cli() {
  if docker ps --format '{{.Names}}' 2>/dev/null | rg -q "^${REDIS_CONTAINER}$"; then
    docker exec "$REDIS_CONTAINER" redis-cli "$@"
  elif command -v redis-cli >/dev/null 2>&1; then
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" "$@"
  else
    echo "redis-cli unavailable" >&2
    return 127
  fi
}

print_manual_steps() {
  cat <<'EOF'
==> 手动双实例验收步骤

1) 启动 Redis（与两实例同 Docker network 或同主机端口）:
   docker run -d --name cat2bug-redis-smoke --network cat2bug-network -p 6379:6379 redis:7-alpine

2) 启动实例 A（端口 2020），完成安装向导，缓存选 Redis，spring.redis.host=redis（或 127.0.0.1）

3) 启动实例 B（端口 2021），共用同一 MySQL/数据目录策略按运维要求；缓存同样选 Redis

4) 实例 A 登录获取 token（或仅 GET /captchaImage 写入 verifyCode region）

5) 在 Redis 中确认 key 存在:
   redis-cli KEYS '*verifyCode*'
   redis-cli KEYS '*token*'

6) 实例 B 使用相同 captcha uuid / token 应能读取（跨实例共享）

7) 清理:
   docker rm -f cat2bug-redis-smoke

自动化辅助:
   ./deploy/test/native-redis-smoke.sh --with-redis --expect-redis http://127.0.0.1:2020
EOF
}

probe_redis_keys() {
  echo "==> Redis key probe @ ${REDIS_HOST}:${REDIS_PORT}"
  for pattern in '*verifyCode*' '*token*' 'j2cache*'; do
    keys=$(redis_cli --raw KEYS "$pattern" 2>/dev/null || true)
    if [[ -n "$keys" ]]; then
      echo "--- KEYS $pattern ---"
      echo "$keys"
    fi
  done
}

if [[ "$PROBE" -eq 1 ]]; then
  probe_redis_keys
  exit 0
fi

print_manual_steps
echo
echo "==> 快速探测（实例 A captcha + Redis KEYS）"
if curl -sf "$BASE_A/captchaImage" >/dev/null 2>&1; then
  echo "OK   instance A reachable @ $BASE_A"
  curl -sS "$BASE_A/captchaImage" >/dev/null || true
  probe_redis_keys
else
  echo "SKIP instance A not reachable @ $BASE_A — follow manual steps above"
fi

if curl -sf "$BASE_B/captchaImage" >/dev/null 2>&1; then
  echo "OK   instance B reachable @ $BASE_B"
else
  echo "INFO instance B not running @ $BASE_B (optional for full cross-instance test)"
fi

echo
echo "See readme/spring-native-delivery/J2CACHE-NATIVE.md"

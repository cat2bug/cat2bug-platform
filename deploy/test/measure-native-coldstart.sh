#!/usr/bin/env bash
# 测量 Native ELF 冷启动（docker run → curl /version）
#
# 用法:
#   ./deploy/test/measure-native-coldstart.sh raw 3              # arm64 raw
#   ARCH=amd64 ./deploy/test/measure-native-coldstart.sh upx 3 # amd64 UPX
#
# 环境变量: ARCH (arm64|amd64)、PORT、DATA_VOL、CONFIG_VOL
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
MODE="${1:-raw}"
RUNS="${2:-3}"
ARCH="${ARCH:-arm64}"
PORT="${PORT:-2029}"
CONTAINER="${CONTAINER:-cat2bug-coldstart-measure}"
DATA_VOL="${DATA_VOL:-cat2bug-native-coldstart-data}"
CONFIG_VOL="${CONFIG_VOL:-cat2bug-native-coldstart-config}"
STAGE="$ROOT/deploy/docker/native-spring-minimal/.build-stage"

case "$ARCH" in
  arm64|aarch64) GOARCH=arm64; DOCKER_PLATFORM=linux/arm64 ;;
  amd64|x86_64) GOARCH=amd64; DOCKER_PLATFORM=linux/amd64 ;;
  *) echo "Unsupported ARCH: $ARCH" >&2; exit 1 ;;
esac

case "$MODE" in
  raw) SRC="$ROOT/cat2bug-platform-admin/target/cat2bug-admin-linux-${GOARCH}" ;;
  upx) SRC="$ROOT/cat2bug-platform-admin/target/cat2bug-admin-linux-${GOARCH}.upx" ;;
  *) echo "Usage: $0 [raw|upx] [runs]" >&2; exit 1 ;;
esac
[[ -f "$SRC" ]] || { echo "[ERROR] 不存在: $SRC" >&2; exit 1; }

mkdir -p "$STAGE"
cp -f "$SRC" "$STAGE/cat2bug-admin"
chmod +x "$STAGE/cat2bug-admin"

docker volume create "$DATA_VOL" >/dev/null
docker volume create "$CONFIG_VOL" >/dev/null
if ! docker run --rm -v "${DATA_VOL}:/dest" debian:bookworm-slim sh -c 'ls -A /dest 2>/dev/null | grep -q .'; then
  docker run --rm -v cat2bug-spring-native-data:/from:ro -v "$DATA_VOL:/to" debian:bookworm-slim sh -c 'cp -a /from/. /to/'
  docker run --rm -v cat2bug-spring-native-config:/from:ro -v "$CONFIG_VOL:/to" debian:bookworm-slim sh -c 'cp -a /from/. /to/'
fi

measure_once() {
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
  sleep 2
  local t0 t1
  t0=$(python3 -c 'import time; print(time.time())')
  docker run -d --name "$CONTAINER" --platform "$DOCKER_PLATFORM" \
    -w /app -p "${PORT}:2020" \
    -v "$STAGE/cat2bug-admin:/cat2bug-admin:ro" \
    -v "$DATA_VOL:/app/data" \
    -v "$CONFIG_VOL:/app/config/install" \
    -e CAT2BUG_UPGRADE_SKIP=true \
    debian:bookworm-slim /cat2bug-admin --server.port=2020 >/dev/null
  for _ in $(seq 1 250); do
    if curl -sf --max-time 2 "http://127.0.0.1:${PORT}/version" >/dev/null 2>&1; then
      t1=$(python3 -c 'import time; print(time.time())')
      python3 -c "print(round($t1-$t0,2))"
      return 0
    fi
    sleep 0.2
  done
  echo "[ERROR] /version 超时" >&2
  docker logs "$CONTAINER" 2>&1 | tail -20 >&2
  return 1
}

echo "==> $MODE $GOARCH cold start ($RUNS runs) @ :$PORT platform=$DOCKER_PLATFORM"
results=()
for i in $(seq 1 "$RUNS"); do
  r=$(measure_once)
  echo "run$i=$r"
  results+=("$r")
done
sleep 3
RSS=$(docker stats "$CONTAINER" --no-stream --format '{{.MemUsage}}' 2>/dev/null || echo n/a)
docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
python3 -c "
vals=[float(x) for x in '''${results[*]}'''.split()]
print('avg', round(sum(vals)/len(vals),2), 'min', min(vals), 'max', max(vals))
print('RSS', '''$RSS''')
"

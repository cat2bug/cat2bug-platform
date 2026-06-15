#!/usr/bin/env bash
# 测量 Native ELF 冷启动（docker run → curl /version）
#
# 用法:
#   ./deploy/test/measure-native-coldstart.sh raw 3                    # 默认 arm64 raw
#   ARCH=amd64 ./deploy/test/measure-native-coldstart.sh upx 3       # amd64 UPX（需 linux-amd64 二进制）
#   ARCH=amd64 ./deploy/test/measure-native-coldstart.sh raw 3       # amd64 raw（见下）
#
# amd64 raw 测量说明:
#   - 二进制须为 x86_64 Linux ELF：`./deploy/build-native-spring.sh x86_64`（或在 CI 下载 artifact）。
#   - 在 x86_64 Linux 上执行（GitHub Actions `ubuntu-latest` 矩阵 job 即 amd64）:
#       ARCH=amd64 ./deploy/test/measure-native-coldstart.sh raw 3
#   - 脚本通过 DOCKER_PLATFORM=linux/amd64 在容器内跑原生 amd64 二进制；Apple Silicon 上测 amd64 需
#     已有 cat2bug-admin-linux-amd64 且 Docker 可拉取/运行 linux/amd64 镜像（较慢，正式数据以 CI 为准）。
#
# arm64 Mac 开发机:
#   - 本机默认 ARCH=arm64，可测 arm64 raw/UPX。
#   - amd64 raw 冷启动/RSS 见 readme/spring-native-delivery/METRICS.md「amd64 raw 待 CI ubuntu-latest 实测」行；
#     CI workflow `spring-native` 在 ubuntu-latest 构建并上传 `cat2bug-admin-spring-x86_64` artifact 后，
#     在 runner 或下载 artifact 的 x86_64 Linux 上跑上述命令，将结果回填 METRICS.md。
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

HOST_UNAME="$(uname -m)"
if [[ "$HOST_UNAME" == "arm64" && "$ARCH" == "amd64" && "$MODE" == "raw" ]]; then
  echo "[NOTE] 当前为 arm64 Mac，amd64 raw 正式度量请在 ubuntu-latest CI 或 x86_64 Linux 执行:" >&2
  echo "       ARCH=amd64 ./deploy/test/measure-native-coldstart.sh raw 3" >&2
  echo "       占位见 readme/spring-native-delivery/METRICS.md" >&2
fi

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

#!/usr/bin/env bash
# 在最小 Linux Docker 中运行 Spring Native（含 .upx）
#
# 用法:
#   ./deploy/docker/run-native-spring-minimal.sh              # 前台 run（minimal scratch）
#   ./deploy/docker/run-native-spring-minimal.sh run debian   # debian:bookworm-slim（含 shell，便于调试）
#   ./deploy/docker/run-native-spring-minimal.sh run-bg       # 后台 + smoke（默认 minimal）
#   ./deploy/docker/run-native-spring-minimal.sh run-bg debian  # 挂载二进制到 debian（~97MB 基础层，仅调试）
#   ./deploy/docker/run-native-spring-minimal.sh pack           # 构建 minimal 镜像并后台启动（默认）
#   ./deploy/docker/run-native-spring-minimal.sh pack minimal   # scratch + glibc + zlib（体积最小）
#   ./deploy/docker/run-native-spring-minimal.sh pack distroless
#   ./deploy/docker/run-native-spring-minimal.sh smoke        # 对已运行容器探测
#   ./deploy/docker/run-native-spring-minimal.sh build        # 构建 distroless 镜像
#   ./deploy/docker/run-native-spring-minimal.sh upx [binary] # 容器内 UPX 压缩 ELF
#
# 镜像体积（arm64 + UPX 二进制 ~94MB 参考）:
#   minimal     约  96MB  默认 pack：scratch + glibc + zlib + CA（比 debian 基础层更小）
#   distroless  约 130MB  glibc 运行时更全，无 shell
#   debian      约  97MB  仅基础层；run-bg debian 另挂载二进制，适合调试
#
# 环境变量:
#   BIN / PORT / ARCH / DOCKER_PLATFORM / BASE
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ADMIN="$ROOT/cat2bug-platform-admin"
DOCKER_DIR="$ROOT/deploy/docker/native-spring-minimal"
# shellcheck source=../scripts/upx-docker.sh
source "$ROOT/deploy/scripts/upx-docker.sh"

MODE="${1:-run}"
BASE="${2:-minimal}"

gnu_arch_for() {
  case "$1" in
    amd64) echo x86_64 ;;
    arm64) echo aarch64 ;;
    *) echo "Unsupported GOARCH: $1" >&2; exit 1 ;;
  esac
}

ld_linux_for() {
  case "$1" in
    amd64) echo ld-linux-x86-64.so.2 ;;
    arm64) echo ld-linux-aarch64.so.1 ;;
    *) echo "Unsupported GOARCH: $1" >&2; exit 1 ;;
  esac
}

ld_linux_dir_for() {
  case "$1" in
    amd64) echo /lib64 ;;
    arm64) echo /lib ;;
    *) echo "Unsupported GOARCH: $1" >&2; exit 1 ;;
  esac
}

HOST_ARCH="$(uname -m)"
case "${ARCH:-$HOST_ARCH}" in
  x86_64|amd64) GOARCH=amd64; DOCKER_PLATFORM="${DOCKER_PLATFORM:-linux/amd64}" ;;
  aarch64|arm64) GOARCH=arm64; DOCKER_PLATFORM="${DOCKER_PLATFORM:-linux/arm64}" ;;
  *) echo "Unsupported ARCH: ${ARCH:-$HOST_ARCH}" >&2; exit 1 ;;
esac

GNU_ARCH="$(gnu_arch_for "$GOARCH")"
LD_LINUX="$(ld_linux_for "$GOARCH")"
LD_LINUX_DIR="$(ld_linux_dir_for "$GOARCH")"

DEFAULT_UPX="$ADMIN/target/cat2bug-admin-linux-${GOARCH}.upx"
DEFAULT_RAW="$ADMIN/target/cat2bug-admin-linux-${GOARCH}"
DEFAULT_SRC="$ADMIN/target/cat2bug-admin"

resolve_bin() {
  BIN="${BIN:-}"
  if [[ -n "$BIN" ]]; then
    [[ -f "$BIN" ]] || { echo "[ERROR] BIN 不存在: $BIN" >&2; exit 1; }
    return 0
  fi
  if [[ -f "$DEFAULT_UPX" ]]; then
    BIN="$DEFAULT_UPX"
  elif [[ -f "$DEFAULT_RAW" ]]; then
    BIN="$DEFAULT_RAW"
  elif [[ -f "$DEFAULT_SRC" ]] && [[ "$(file -b "$DEFAULT_SRC")" == *"ELF"* ]]; then
    BIN="$DEFAULT_SRC"
  else
    echo "[ERROR] 未找到 Native ELF，请先构建:" >&2
    echo "  SKIP_EMBEDDED=true ./deploy/build-native-spring.sh ${ARCH:-aarch64}" >&2
    echo "  或 CONTAINER_BUILD=false 本机构建后仅对 ELF 使用本脚本" >&2
    exit 1
  fi
}

PORT="${PORT:-2020}"
IMAGE="cat2bug-spring-native-minimal:${GOARCH}"
STAGE="$DOCKER_DIR/.build-stage"

container_app_args() {
  if [[ "$BASE" == "minimal" ]]; then
    echo --server.port=2020 --server.tomcat.basedir=/app/tmp -Djava.io.tmpdir=/app/tmp
  else
    echo --server.port=2020
  fi
}

prepare_stage() {
  resolve_bin
  rm -rf "$STAGE"
  mkdir -p "$STAGE"
  cp -f "$BIN" "$STAGE/cat2bug-admin"
  chmod +x "$STAGE/cat2bug-admin"
}

build_image() {
  local base_image dockerfile
  case "$BASE" in
    debian) base_image="debian:bookworm-slim" ;;
    distroless) base_image="gcr.io/distroless/cc-debian12:latest" ;;
    minimal|*) base_image="scratch"; BASE="minimal" ;;
  esac
  echo "==> 构建镜像 $IMAGE (platform=$DOCKER_PLATFORM, base=$BASE)"
  prepare_stage
  if [[ "$BASE" == "debian" ]]; then
    docker build --platform "$DOCKER_PLATFORM" -t "$IMAGE" -f- "$STAGE" <<EOF
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates zlib1g \\
    && rm -rf /var/lib/apt/lists/*
COPY cat2bug-admin /cat2bug-admin
WORKDIR /app
EXPOSE 2020
ENTRYPOINT ["/cat2bug-admin"]
EOF
  elif [[ "$BASE" == "minimal" ]]; then
    cp "$DOCKER_DIR/Dockerfile.minimal" "$STAGE/Dockerfile"
    docker build --platform "$DOCKER_PLATFORM" \
      --build-arg "GNU_ARCH=$GNU_ARCH" \
      --build-arg "LD_LINUX=$LD_LINUX" \
      --build-arg "LD_LINUX_DIR=$LD_LINUX_DIR" \
      -t "$IMAGE" "$STAGE"
  else
    cp "$DOCKER_DIR/Dockerfile" "$STAGE/Dockerfile"
    docker build --platform "$DOCKER_PLATFORM" \
      --build-arg "GNU_ARCH=$GNU_ARCH" \
      -t "$IMAGE" "$STAGE"
  fi
  echo "==> 镜像大小:"
  docker images "$IMAGE" --format '{{.Repository}}:{{.Tag}}  {{.Size}}'
}

run_container() {
  local name="cat2bug-spring-native-$$"
  cleanup() { docker rm -f "$name" >/dev/null 2>&1 || true; }
  trap cleanup EXIT

  if [[ "$BASE" == "debian" && "$MODE" == "run" ]]; then
    echo "==> 一次性运行 (debian:bookworm-slim, platform=$DOCKER_PLATFORM)"
    echo "    BIN=$BIN"
    prepare_stage
    docker run --rm --name "$name" --platform "$DOCKER_PLATFORM" \
      -w /app \
      -p "${PORT}:2020" \
      -v "$STAGE/cat2bug-admin:/cat2bug-admin:ro" \
      -v cat2bug-spring-native-data:/app/data \
      -v cat2bug-spring-native-upload:/app/upload \
      -v cat2bug-spring-native-config:/app/config/install \
      debian:bookworm-slim \
      /cat2bug-admin --server.port=2020
    return
  fi

  if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then
    build_image
  fi

  echo "==> 启动容器 $IMAGE (http://127.0.0.1:${PORT})"
  # shellcheck disable=SC2046
  docker run --rm --name "$name" --platform "$DOCKER_PLATFORM" \
    -w /app \
    -p "${PORT}:2020" \
    -v cat2bug-spring-native-data:/app/data \
    -v cat2bug-spring-native-upload:/app/upload \
    -v cat2bug-spring-native-config:/app/config/install \
    "$IMAGE" $(container_app_args)
}

smoke() {
  local url="http://127.0.0.1:${PORT}"
  echo "==> 等待 ${url}/version ..."
  for _ in $(seq 1 180); do
    if curl -sf "${url}/version" >/dev/null 2>&1; then
      echo "version: $(curl -sf "${url}/version")"
      return 0
    fi
    sleep 0.5
  done
  echo "[ERROR] /version 超时" >&2
  return 1
}

cmd_upx() {
  local src="${1:-}"
  if [[ -z "$src" ]]; then
    if [[ -f "$DEFAULT_RAW" ]]; then
      src="$DEFAULT_RAW"
    elif [[ -f "$DEFAULT_SRC" ]] && [[ "$(file -b "$DEFAULT_SRC")" == *"ELF"* ]]; then
      src="$DEFAULT_SRC"
    else
      echo "[ERROR] 请指定 Linux ELF 路径，或先构建 $DEFAULT_RAW" >&2
      exit 1
    fi
  fi
  local out="${src}.upx"
  DOCKER_PLATFORM="$DOCKER_PLATFORM" upx_compress_in_docker "$src" "$out"
  echo ""
  echo "运行 UPX 包: UPX_IN_DOCKER=true BIN=$out $0 run-bg debian"
}

case "$MODE" in
  build) build_image ;;
  pack)
    BASE="${2:-minimal}"
    build_image
    MODE=run-bg
    local_name="cat2bug-spring-native-minimal"
    docker rm -f "$local_name" >/dev/null 2>&1 || true
    resolve_bin
    if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then build_image; fi
    # shellcheck disable=SC2046
    docker run -d --restart unless-stopped --name "$local_name" --platform "$DOCKER_PLATFORM" \
      -w /app \
      -p "${PORT}:2020" \
      -v cat2bug-spring-native-data:/app/data \
      -v cat2bug-spring-native-upload:/app/upload \
      -v cat2bug-spring-native-config:/app/config/install \
      "$IMAGE" $(container_app_args)
    smoke
    echo "镜像: $IMAGE"
    docker images "$IMAGE" --format '  大小: {{.Size}}'
    echo "访问: http://127.0.0.1:${PORT}"
    echo "停止: docker rm -f $local_name"
    ;;
  upx) cmd_upx "${2:-}" ;;
  run)
    resolve_bin
    echo "访问: http://127.0.0.1:${PORT}/version"
    run_container
    ;;
  run-bg)
    local_name="cat2bug-spring-native-minimal"
    docker rm -f "$local_name" >/dev/null 2>&1 || true
    resolve_bin
    if [[ "$BASE" == "debian" ]]; then
      prepare_stage
      docker run -d --restart unless-stopped --name "$local_name" --platform "$DOCKER_PLATFORM" \
        -w /app \
        -p "${PORT}:2020" \
        -v "$STAGE/cat2bug-admin:/cat2bug-admin:ro" \
        -v cat2bug-spring-native-data:/app/data \
        -v cat2bug-spring-native-upload:/app/upload \
        -v cat2bug-spring-native-config:/app/config/install \
        debian:bookworm-slim /cat2bug-admin --server.port=2020
    else
      if ! docker image inspect "$IMAGE" >/dev/null 2>&1; then build_image; fi
      # shellcheck disable=SC2046
      docker run -d --restart unless-stopped --name "$local_name" --platform "$DOCKER_PLATFORM" \
        -w /app \
        -p "${PORT}:2020" \
        -v cat2bug-spring-native-data:/app/data \
        -v cat2bug-spring-native-upload:/app/upload \
        -v cat2bug-spring-native-config:/app/config/install \
        "$IMAGE" $(container_app_args)
    fi
    smoke
    echo "停止: docker rm -f $local_name"
    ;;
  smoke) smoke ;;
  stop) docker rm -f cat2bug-spring-native-minimal 2>/dev/null || true ;;
  *)
    echo "Usage: $0 [run|run-bg|pack|build|smoke|stop|upx] [minimal|distroless|debian] [elf-binary]" >&2
    exit 1
    ;;
esac

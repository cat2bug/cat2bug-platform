#!/usr/bin/env bash
# 从 Linux ELF（或 .upx）构建 cat2bug Spring Native 最小运行时 Docker 镜像
#
# 用法:
#   ./deploy/docker/build-cat2bug-spring-upx-image.sh
#   BIN=cat2bug-platform-admin/target/cat2bug-admin-linux-arm64.upx ./deploy/docker/build-cat2bug-spring-upx-image.sh
#   ./deploy/docker/build-cat2bug-spring-upx-image.sh build-and-upx   # ELF → Docker UPX → 镜像
#
# 环境变量: ARCH / DOCKER_PLATFORM / BIN / IMAGE_TAG
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ADMIN="$ROOT/cat2bug-platform-admin"
# shellcheck source=../scripts/upx-docker.sh
source "$ROOT/deploy/scripts/upx-docker.sh"

MODE="${1:-build}"
BASE="${2:-${DOCKER_BASE:-minimal}}"

HOST_ARCH="$(uname -m)"
case "${ARCH:-$HOST_ARCH}" in
  x86_64|amd64) GOARCH=amd64; DOCKER_PLATFORM="${DOCKER_PLATFORM:-linux/amd64}" ;;
  aarch64|arm64) GOARCH=arm64; DOCKER_PLATFORM="${DOCKER_PLATFORM:-linux/arm64}" ;;
  *) echo "Unsupported ARCH: ${ARCH:-$HOST_ARCH}" >&2; exit 1 ;;
esac

IMAGE_TAG="${IMAGE_TAG:-cat2bug-spring-native-upx:${GOARCH}}"
DEFAULT_RAW="$ADMIN/target/cat2bug-admin-linux-${GOARCH}"
DEFAULT_UPX="${DEFAULT_RAW}.upx"
DEFAULT_ELF="$ADMIN/target/cat2bug-admin"

resolve_elf() {
  if [[ -f "$DEFAULT_RAW" ]]; then
    echo "$DEFAULT_RAW"
  elif [[ -f "$DEFAULT_ELF" ]] && [[ "$(file -b "$DEFAULT_ELF")" == *"ELF"* ]]; then
    echo "$DEFAULT_ELF"
  else
    return 1
  fi
}

ld_linux_for() {
  case "$GOARCH" in
    amd64) echo ld-linux-x86-64.so.2 ;;
    arm64) echo ld-linux-aarch64.so.1 ;;
    *) echo "[ERROR] 未知 GOARCH: $GOARCH" >&2; exit 1 ;;
  esac
}

ld_linux_dir_for() {
  case "$GOARCH" in
    amd64) echo /lib64 ;;
    arm64) echo /lib ;;
    *) echo "[ERROR] 未知 GOARCH: $GOARCH" >&2; exit 1 ;;
  esac
}

minimal_run_hint() {
  if [[ "$BASE" == "minimal" ]]; then
    echo "运行: docker run --rm -p 2020:2020 -v cat2bug-spring-native-data:/app/data $IMAGE_TAG --server.port=2020 --server.tomcat.basedir=/app/tmp -Djava.io.tmpdir=/app/tmp"
  else
    echo "运行: docker run --rm -p 2020:2020 -v cat2bug-spring-native-data:/app/data $IMAGE_TAG --server.port=2020"
  fi
}

build_runtime_image() {
  local bin="${BIN:-}"
  if [[ -z "$bin" ]]; then
    if [[ -f "$DEFAULT_UPX" ]]; then
      bin="$DEFAULT_UPX"
    elif [[ -f "$DEFAULT_RAW" ]]; then
      bin="$DEFAULT_RAW"
    else
      echo "[ERROR] 未找到 UPX/ELF，请先构建 Native 或指定 BIN=" >&2
      echo "  SKIP_EMBEDDED=true ./deploy/build-native-spring.sh ${ARCH:-aarch64}" >&2
      exit 1
    fi
  fi
  [[ -f "$bin" ]] || { echo "[ERROR] BIN 不存在: $bin" >&2; exit 1; }

  local stage="$ROOT/deploy/docker/native-spring-minimal/.build-stage"
  rm -rf "$stage"
  mkdir -p "$stage"
  cp -f "$bin" "$stage/cat2bug-admin"
  chmod +x "$stage/cat2bug-admin"

  echo "==> 构建运行时镜像 $IMAGE_TAG (platform=$DOCKER_PLATFORM, base=$BASE, bin=$(du -h "$bin" | cut -f1))"
  local gnu_arch ld_linux ld_linux_dir
  case "$GOARCH" in
    amd64) gnu_arch=x86_64 ;;
    arm64) gnu_arch=aarch64 ;;
    *) echo "[ERROR] 未知 GOARCH: $GOARCH" >&2; exit 1 ;;
  esac
  ld_linux="$(ld_linux_for)"
  ld_linux_dir="$(ld_linux_dir_for)"
  if [[ "$BASE" == "debian" ]]; then
    docker build --platform "$DOCKER_PLATFORM" -t "$IMAGE_TAG" -f- "$stage" <<EOF
FROM debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \\
    && rm -rf /var/lib/apt/lists/*
COPY cat2bug-admin /cat2bug-admin
WORKDIR /app
EXPOSE 2020
ENTRYPOINT ["/cat2bug-admin"]
EOF
  elif [[ "$BASE" == "minimal" ]]; then
    cp "$ROOT/deploy/docker/native-spring-minimal/Dockerfile.minimal" "$stage/Dockerfile"
    docker build --platform "$DOCKER_PLATFORM" \
      --build-arg "GNU_ARCH=${gnu_arch}" \
      --build-arg "LD_LINUX=${ld_linux}" \
      --build-arg "LD_LINUX_DIR=${ld_linux_dir}" \
      -t "$IMAGE_TAG" "$stage"
  elif [[ "$BASE" == "distroless" ]]; then
    cp "$ROOT/deploy/docker/native-spring-minimal/Dockerfile" "$stage/Dockerfile"
    docker build --platform "$DOCKER_PLATFORM" \
      --build-arg "GNU_ARCH=${gnu_arch}" \
      --build-arg "BASE_IMAGE=gcr.io/distroless/cc-debian12:latest" \
      -t "$IMAGE_TAG" "$stage"
  else
    echo "[ERROR] 未知 BASE: $BASE (可用: minimal|distroless|debian)" >&2
    exit 1
  fi
  docker images "$IMAGE_TAG" --format '{{.Repository}}:{{.Tag}}  {{.Size}}'
  echo ""
  minimal_run_hint
}

build_and_upx() {
  local elf
  if ! elf="$(resolve_elf)"; then
    echo "[ERROR] 需要 Linux ELF，请先:" >&2
    echo "  SKIP_EMBEDDED=true ./deploy/build-native-spring.sh ${ARCH:-aarch64}" >&2
    exit 1
  fi
  local out="${elf%.upx}"
  [[ "$out" == "$elf" ]] || elf="${out}"
  local upx_out="${elf}.upx"
  DOCKER_PLATFORM="$DOCKER_PLATFORM" upx_compress_in_docker "$elf" "$upx_out"
  BIN="$upx_out" build_runtime_image
}

case "$MODE" in
  build) build_runtime_image ;;
  build-and-upx) build_and_upx ;;
  *)
    echo "Usage: $0 [build|build-and-upx] [minimal|distroless|debian]" >&2
    exit 1
    ;;
esac

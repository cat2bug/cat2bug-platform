#!/usr/bin/env bash
# Spring Boot Native 双架构构建（linux/amd64 | linux/arm64，embedded SPA）
# 需 Docker（GraalVM native-image 容器）；内嵌 SPA 需 Node 构建前端。
#
# 用法:
#   ./deploy/build-native-spring.sh              # 本机架构
#   ./deploy/build-native-spring.sh x86_64       # linux/amd64
#   ./deploy/build-native-spring.sh aarch64      # linux/arm64
#   SKIP_EMBEDDED=true ./deploy/build-native-spring.sh   # 跳过前端（非默认 Release）
#   UPX_COMPRESS=false ./deploy/build-native-spring.sh   # 不生成 .upx
#   UPX_IN_DOCKER=true ./deploy/build-native-spring.sh       # Linux ELF 在 debian 容器内 UPX
#   CONTAINER_BUILD=false ./deploy/build-native-spring.sh  # 本机 GraalVM（需已安装）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=scripts/upx-args.sh
source "$ROOT/deploy/scripts/upx-args.sh"
# shellcheck source=scripts/upx-docker.sh
source "$ROOT/deploy/scripts/upx-docker.sh"
ADMIN="$ROOT/cat2bug-platform-admin"
ARCH="${1:-$(uname -m)}"
CONTAINER_BUILD="${CONTAINER_BUILD:-true}"
NATIVE_BUILDER_IMAGE="${NATIVE_BUILDER_IMAGE:-ghcr.io/graalvm/native-image-community:21-ol9}"

case "$ARCH" in
  x86_64|amd64)
    GOARCH=amd64
    DOCKER_PLATFORM=linux/amd64
    ;;
  aarch64|arm64)
    GOARCH=arm64
    DOCKER_PLATFORM=linux/arm64
    ;;
  *)
    echo "Unsupported arch: $ARCH (use x86_64 or aarch64)" >&2
    exit 1
    ;;
esac

if [[ "${SKIP_EMBEDDED:-false}" != "true" ]]; then
  echo "==> 前端 build:embedded"
  "$ROOT/deploy/scripts/ui-build.sh" embedded
else
  echo "==> API-only Native（非默认 Release 路径）"
fi

NATIVE_PROFILES=(-Pnative -Pembedded -Prelease)
MVN_GOAL=(clean package)

if [[ "$CONTAINER_BUILD" == "true" ]]; then
  HOST_ARCH="$(uname -m)"
  NEED_PLATFORM=false
  case "$ARCH" in
    x86_64|amd64) [[ "$HOST_ARCH" != "x86_64" ]] && NEED_PLATFORM=true ;;
    aarch64|arm64) [[ "$HOST_ARCH" != "arm64" && "$HOST_ARCH" != "aarch64" ]] && NEED_PLATFORM=true ;;
  esac
  DOCKER_RUN=(docker run --rm --entrypoint /bin/bash)
  export DOCKER_DEFAULT_PLATFORM="$DOCKER_PLATFORM"
  DOCKER_RUN+=(--platform "$DOCKER_PLATFORM")
  echo "==> Docker platform: $DOCKER_PLATFORM"
  DOCKER_RUN+=(
    -v "$ROOT:/project"
    -v "${HOME}/.m2:/root/.m2"
    -w /project
    "$NATIVE_BUILDER_IMAGE"
  )
  MVN_CMD="microdnf install -y maven tar gzip >/dev/null 2>&1 || true; mvn ${NATIVE_PROFILES[*]} -pl cat2bug-platform-admin -am ${MVN_GOAL[*]} -DskipTests -Dnative.image.jvmargs='-J-Xmx8g -H:DeadlockWatchdogInterval=7200' -Dnative.image.threads=4"
  echo "==> Spring Native build in container: $NATIVE_BUILDER_IMAGE"
  echo "[HINT] Docker Desktop 内存建议 ≥12GB（amd64 交叉编译建议 ≥16GB）；当前若不足可能在 native-image 阶段 OOM（exit 137）"
  if ! "${DOCKER_RUN[@]}" -lc "$MVN_CMD"; then
    echo "" >&2
    echo "[HINT] 容器内 Native 构建失败。可尝试本机构建: CONTAINER_BUILD=false $0 $ARCH" >&2
    exit 1
  fi
else
  echo "==> Spring Native build (local GraalVM)"
  MVN_ARGS=(
    "${NATIVE_PROFILES[@]}"
    -pl cat2bug-platform-admin -am
    "${MVN_GOAL[@]}" -DskipTests
    -Dnative.image.jvmargs=-J-Xmx8g
  )
  if ! (cd "$ROOT" && mvn "${MVN_ARGS[@]}"); then
    NATIVE_LOG_DIR="$ADMIN/target"
    echo "" >&2
    echo "[HINT] Spring Native 构建失败。常见原因：MyBatis/Security/POI 缺少 RuntimeHints。" >&2
    echo "  日志目录: $NATIVE_LOG_DIR" >&2
    echo "  开发回归: mvn -pl cat2bug-platform-admin -am package -DskipTests" >&2
    echo "  参见: readme/spring-native-delivery/PHASE-1.md" >&2
    exit 1
  fi
fi

SRC="$ADMIN/target/cat2bug-admin"
if [[ ! -f "$SRC" ]]; then
  echo "[ERROR] Native binary not found: $SRC" >&2
  exit 1
fi

# 按实际二进制平台命名（本机构建可能是 Mach-O，容器构建为 ELF）
BINARY_INFO="$(file -b "$SRC")"
if [[ "$BINARY_INFO" == *"ELF"* ]]; then
  OUT="$ADMIN/target/cat2bug-admin-linux-${GOARCH}"
elif [[ "$BINARY_INFO" == *"Mach-O"* ]]; then
  OUT="$ADMIN/target/cat2bug-admin-$(uname -s | tr '[:upper:]' '[:lower:]')-$(uname -m)"
else
  OUT="$ADMIN/target/cat2bug-admin-native"
fi
cp -f "$SRC" "$OUT"
chmod +x "$OUT"

if [[ "$BINARY_INFO" == *"ELF"* ]]; then
  "$ROOT/deploy/scripts/verify-native-no-awt.sh" "$OUT" || true
  "$ROOT/deploy/scripts/verify-native-no-kaptcha.sh" "$OUT" || true
  [[ -x "$ROOT/deploy/scripts/verify-native-no-poi.sh" ]] && \
    "$ROOT/deploy/scripts/verify-native-no-poi.sh" "$OUT" || true
fi

RAW_SIZE="$(du -h "$OUT" | cut -f1)"
echo ""
echo "Done: $OUT ($RAW_SIZE)"

UPX_COMPRESS="${UPX_COMPRESS:-true}"
if [[ "$UPX_COMPRESS" == "true" ]]; then
  OUT_UPX="${OUT}.upx"
  compress_upx=false
  if [[ "$BINARY_INFO" == *"ELF"* ]]; then
    case "${UPX_IN_DOCKER:-auto}" in
      true|1|yes) compress_upx=docker ;;
      false|0|no)
        command -v upx >/dev/null 2>&1 && compress_upx=host
        ;;
      auto)
        if [[ "$(uname -s)" == "Darwin" ]] || ! command -v upx >/dev/null 2>&1; then
          compress_upx=docker
        else
          compress_upx=host
        fi
        ;;
    esac
  elif command -v upx >/dev/null 2>&1; then
    compress_upx=host
  fi

  if [[ "$compress_upx" == "docker" ]]; then
    DOCKER_PLATFORM="$DOCKER_PLATFORM" upx_compress_in_docker "$OUT" "$OUT_UPX"
  elif [[ "$compress_upx" == "host" ]]; then
    cp -f "$OUT" "$OUT_UPX"
    chmod +x "$OUT_UPX"
    read -r -a UPX_ARGS <<< "$(upx_build_args)"
    if [[ "$(uname -s)" == "Darwin" ]]; then
      UPX_ARGS+=(--force-macos)
    fi
    echo "==> UPX compress (${UPX_MODE:-best}): $OUT_UPX (保留未压缩 $OUT 供 RPM/签名)"
    if upx "${UPX_ARGS[@]}" "$OUT_UPX"; then
      RAW_BYTES="$(wc -c < "$OUT" | tr -d ' ')"
      UPX_BYTES="$(wc -c < "$OUT_UPX" | tr -d ' ')"
      UPX_RATIO="$(awk "BEGIN { printf \"%.2f\", ($UPX_BYTES / $RAW_BYTES) * 100 }")"
      RAW_SIZE="$(du -h "$OUT" | cut -f1)"
      UPX_SIZE="$(du -h "$OUT_UPX" | cut -f1)"
      echo "UPX:  $OUT_UPX ($UPX_SIZE, ${UPX_RATIO}% of raw $RAW_SIZE)"
    else
      rm -f "$OUT_UPX"
      echo "[WARN] UPX 压缩失败，已删除 $OUT_UPX" >&2
    fi
  else
    echo "[WARN] 未找到 upx 且未启用 UPX_IN_DOCKER，跳过压缩" >&2
  fi

  if [[ -f "$OUT_UPX" ]]; then
    if [[ "$BINARY_INFO" == *"Mach-O"* ]]; then
      echo "[NOTE] macOS UPX 产物仅用于体积评估；可执行发布物请使用 linux ELF + run-native-spring-minimal.sh"
    fi
    if [[ "$BINARY_INFO" == *"ELF"* && "${UPX_SMOKE:-true}" == "true" ]] && command -v docker >/dev/null 2>&1; then
      SMOKE_PORT="${UPX_SMOKE_PORT:-2023}"
      echo "==> UPX smoke (minimal docker, port $SMOKE_PORT)..."
      if PORT="$SMOKE_PORT" BIN="$OUT_UPX" ARCH="$GOARCH" DOCKER_PLATFORM="$DOCKER_PLATFORM" \
        "$ROOT/deploy/docker/run-native-spring-minimal.sh" run-bg debian; then
        echo "UPX smoke OK: http://127.0.0.1:${SMOKE_PORT}/version"
      else
        echo "[WARN] UPX 最小镜像冒烟失败" >&2
      fi
      docker rm -f cat2bug-spring-native-minimal >/dev/null 2>&1 || true
    fi
  fi
fi

echo "Smoke: $OUT &  curl -sf http://127.0.0.1:2020/version && curl -sf -H 'Accept: text/html' http://127.0.0.1:2020/"

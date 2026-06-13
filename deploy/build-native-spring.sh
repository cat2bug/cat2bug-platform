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
#   CONTAINER_BUILD=false ./deploy/build-native-spring.sh  # 本机 GraalVM（需已安装）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
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
  if [[ "$NEED_PLATFORM" == "true" ]]; then
    export DOCKER_DEFAULT_PLATFORM="$DOCKER_PLATFORM"
    DOCKER_RUN+=(--platform "$DOCKER_PLATFORM")
    echo "==> Docker platform: $DOCKER_PLATFORM"
  fi
  DOCKER_RUN+=(
    -v "$ROOT:/project"
    -v "${HOME}/.m2:/root/.m2"
    -w /project
    "$NATIVE_BUILDER_IMAGE"
  )
  MVN_CMD="microdnf install -y maven tar gzip >/dev/null 2>&1 || true; mvn ${NATIVE_PROFILES[*]} -pl cat2bug-platform-admin -am ${MVN_GOAL[*]} -DskipTests -Dnative.image.jvmargs='-J-Xmx3g -H:DeadlockWatchdogInterval=7200'"
  echo "==> Spring Native build in container: $NATIVE_BUILDER_IMAGE"
  echo "[HINT] Docker Desktop 内存建议 ≥12GB；当前若 <8GB 可能在 native-image 阶段 OOM（exit 137）"
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

RAW_SIZE="$(du -h "$OUT" | cut -f1)"
echo ""
echo "Done: $OUT ($RAW_SIZE)"

UPX_COMPRESS="${UPX_COMPRESS:-true}"
if [[ "$UPX_COMPRESS" == "true" ]]; then
  OUT_UPX="${OUT}.upx"
  cp -f "$OUT" "$OUT_UPX"
  chmod +x "$OUT_UPX"
  if command -v upx >/dev/null 2>&1; then
    UPX_ARGS=(--best --lzma)
    if [[ "$(uname -s)" == "Darwin" ]]; then
      UPX_ARGS+=(--force-macos)
    fi
    echo "==> UPX compress: $OUT_UPX (保留未压缩 $OUT 供 RPM/签名；370MB 量级约需 3–5 分钟)"
    if upx "${UPX_ARGS[@]}" "$OUT_UPX"; then
      RAW_BYTES="$(wc -c < "$OUT" | tr -d ' ')"
      UPX_BYTES="$(wc -c < "$OUT_UPX" | tr -d ' ')"
      UPX_RATIO="$(awk "BEGIN { printf \"%.2f\", ($UPX_BYTES / $RAW_BYTES) * 100 }")"
      RAW_SIZE="$(du -h "$OUT" | cut -f1)"
      UPX_SIZE="$(du -h "$OUT_UPX" | cut -f1)"
      echo "UPX:  $OUT_UPX ($UPX_SIZE, ${UPX_RATIO}% of raw $RAW_SIZE)"
      if [[ "$BINARY_INFO" == *"Mach-O"* ]]; then
        echo "[NOTE] macOS UPX 产物仅用于体积评估；可执行发布物请使用 linux/*.upx（容器构建）。"
      fi
      if [[ "$BINARY_INFO" == *"ELF"* && "${UPX_SMOKE:-true}" == "true" ]] && command -v docker >/dev/null 2>&1; then
        SMOKE_PORT="${UPX_SMOKE_PORT:-2023}"
        echo "==> UPX smoke (docker, port $SMOKE_PORT)..."
        if docker run --rm -d --name cat2bug-upx-smoke \
          -p "${SMOKE_PORT}:${SMOKE_PORT}" \
          -v "$OUT_UPX:/app/cat2bug-admin:ro" \
          rockylinux:9 /app/cat2bug-admin --server.port="${SMOKE_PORT}" >/dev/null 2>&1; then
          for _ in $(seq 1 120); do
            if curl -sf "http://127.0.0.1:${SMOKE_PORT}/version" >/dev/null 2>&1; then
              echo "UPX smoke OK: http://127.0.0.1:${SMOKE_PORT}/version"
              break
            fi
            sleep 2
          done
          docker stop cat2bug-upx-smoke >/dev/null 2>&1 || true
        else
          echo "[WARN] UPX docker 冒烟启动失败" >&2
        fi
      fi
    else
      rm -f "$OUT_UPX"
      echo "[WARN] UPX 压缩失败，已删除 $OUT_UPX" >&2
    fi
  else
    rm -f "$OUT_UPX"
    echo "[WARN] 未找到 upx，跳过压缩。安装: macOS「brew install upx」/ RHEL「dnf install upx」" >&2
    echo "       或设置 UPX_COMPRESS=false" >&2
  fi
fi

echo "Smoke: $OUT &  curl -sf http://127.0.0.1:2020/version && curl -sf -H 'Accept: text/html' http://127.0.0.1:2020/"

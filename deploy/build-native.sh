#!/usr/bin/env bash
# Quarkus Native 双架构构建（linux/amd64 | linux/arm64）
# 需 Docker（Mandrel 容器构建）；内嵌 SPA 需 Node 构建前端。
#
# 用法:
#   ./deploy/build-native.sh              # 本机架构
#   ./deploy/build-native.sh x86_64       # linux/amd64
#   ./deploy/build-native.sh aarch64      # linux/arm64
#   SKIP_EMBEDDED=true ./deploy/build-native.sh   # 跳过前端（API-only）
#   CONTAINER_BUILD=false ./deploy/build-native.sh  # 本机 GraalVM（需已安装）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ADMIN="$ROOT/cat2bug-platform-admin"
ARCH="${1:-$(uname -m)}"
CONTAINER_BUILD="${CONTAINER_BUILD:-true}"

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
  echo "==> 前端 build:embedded-quarkus"
  (cd "$ROOT/cat2bug-platform-ui" && NODE_OPTIONS="${NODE_OPTIONS:---openssl-legacy-provider}" npm run build:embedded-quarkus)
fi

echo "==> Native build (platform=$DOCKER_PLATFORM, container=$CONTAINER_BUILD)"
MVN_ARGS=(
  -Pembedded -Prelease -Pnative
  -pl cat2bug-platform-admin -am
  package -DskipTests
  -Dnative
  -Dquarkus.native.container-build="$CONTAINER_BUILD"
  -Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21
)

if [[ "$CONTAINER_BUILD" == "true" ]]; then
  HOST_ARCH="$(uname -m)"
  NEED_PLATFORM=false
  case "$ARCH" in
    x86_64|amd64) [[ "$HOST_ARCH" != "x86_64" ]] && NEED_PLATFORM=true ;;
    aarch64|arm64) [[ "$HOST_ARCH" != "arm64" && "$HOST_ARCH" != "aarch64" ]] && NEED_PLATFORM=true ;;
  esac
  if [[ "$NEED_PLATFORM" == "true" ]]; then
    MVN_ARGS+=("-Dquarkus.native.container-runtime-options=--platform=${DOCKER_PLATFORM}")
  fi
fi

# Maven 增量复制不会删除旧 hash 的前端文件，残留会被整体嵌入 Native
rm -rf "$ADMIN/target/classes/META-INF/resources"
(cd "$ROOT" && mvn "${MVN_ARGS[@]}")

SRC="$ADMIN/target/cat2bug-admin"
if [[ ! -f "$SRC" ]]; then
  # Quarkus 偶发输出 *-runner 命名
  if [[ -f "$ADMIN/target/cat2bug-admin-runner" ]]; then
    SRC="$ADMIN/target/cat2bug-admin-runner"
  else
    SRC="$(ls "$ADMIN"/target/*-runner 2>/dev/null | head -1 || true)"
  fi
fi
if [[ -z "${SRC:-}" || ! -f "$SRC" ]]; then
  echo "[ERROR] Native binary not found under $ADMIN/target (expected cat2bug-admin)" >&2
  exit 1
fi

OUT="$ADMIN/target/cat2bug-admin-linux-${GOARCH}"
cp -f "$SRC" "$OUT"
chmod +x "$OUT"

RAW_SIZE="$(du -h "$OUT" | cut -f1)"
RAW_BYTES="$(wc -c < "$OUT" | tr -d ' ')"
echo ""
echo "Native raw: $OUT ($RAW_SIZE, ${RAW_BYTES} bytes)"

if [[ "${SKIP_UPX:-false}" != "true" ]]; then
  echo "==> UPX compress (Gate B 体积口径默认按 UPX 后计)"
  UPX_IN_DOCKER="${UPX_IN_DOCKER:-auto}" "$ROOT/deploy/upx-native-binary.sh" "$OUT"
  if [[ -f "${OUT}.upx" ]]; then
    UPX_SIZE="$(du -h "${OUT}.upx" | cut -f1)"
    UPX_BYTES="$(wc -c < "${OUT}.upx" | tr -d ' ')"
    echo "Native upx: ${OUT}.upx ($UPX_SIZE, ${UPX_BYTES} bytes)"
  fi
fi

echo ""
echo "Done: $OUT"
echo "Smoke: ./cat2bug-platform-admin/smoke-native-docker.sh ${GOARCH}"
echo "  or:  $OUT &  curl -sf http://127.0.0.1:2020/q/health/ready && curl -sf http://127.0.0.1:2020/version"

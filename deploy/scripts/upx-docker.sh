#!/usr/bin/env bash
# 在最小 Debian 容器内对 Linux ELF 执行 UPX（macOS 无 upx / 需与发布环境一致的 glibc UPX 时使用）
#
# 用法（由 upx-native-binary.sh / build-native-spring.sh 调用）:
#   upx_compress_in_docker /path/to/cat2bug-admin-linux-arm64 [/path/to/out.upx]
#
# 环境变量:
#   UPX_MODE          best | brute | ultra（见 upx-args.sh）
#   UPX_DOCKER_IMAGE  默认 cat2bug-upx-compress:bookworm-slim（本地 build 一次）
#   DOCKER_PLATFORM   linux/arm64 | linux/amd64（默认按二进制 file(1) 推断）
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck source=upx-args.sh
source "$ROOT/deploy/scripts/upx-args.sh"

UPX_DOCKER_IMAGE="${UPX_DOCKER_IMAGE:-cat2bug-upx-compress:bookworm-slim}"
UPX_DOCKERFILE="${UPX_DOCKERFILE:-$ROOT/deploy/docker/upx-compress/Dockerfile}"

upx_docker_ensure_image() {
  local platform="${1:-}"
  if [[ -n "$platform" ]] && docker image inspect "$UPX_DOCKER_IMAGE" >/dev/null 2>&1; then
    local img_arch
    img_arch="$(docker image inspect "$UPX_DOCKER_IMAGE" --format '{{.Os}}/{{.Architecture}}' 2>/dev/null || true)"
    if [[ -n "$img_arch" && "$img_arch" != "${platform#linux/}" && "$img_arch" != "$platform" ]]; then
      case "$platform" in
        linux/amd64) [[ "$img_arch" == "linux/amd64" ]] || docker rmi -f "$UPX_DOCKER_IMAGE" 2>/dev/null || true ;;
        linux/arm64) [[ "$img_arch" == "linux/arm64" ]] || docker rmi -f "$UPX_DOCKER_IMAGE" 2>/dev/null || true ;;
      esac
    fi
  fi
  if docker image inspect "$UPX_DOCKER_IMAGE" >/dev/null 2>&1; then
    return 0
  fi
  echo "==> 构建 UPX 压缩镜像: $UPX_DOCKER_IMAGE${platform:+ ($platform)}"
  if [[ -n "$platform" ]]; then
    docker build --platform "$platform" -t "$UPX_DOCKER_IMAGE" -f "$UPX_DOCKERFILE" \
      "$(dirname "$UPX_DOCKERFILE")"
  else
    docker build -t "$UPX_DOCKER_IMAGE" -f "$UPX_DOCKERFILE" \
      "$(dirname "$UPX_DOCKERFILE")"
  fi
}

upx_docker_detect_platform() {
  local info="$1"
  if [[ "$info" == *"aarch64"* || "$info" == *"ARM"* ]]; then
    echo linux/arm64
  elif [[ "$info" == *"x86-64"* || "$info" == *"80386"* ]]; then
    echo linux/amd64
  else
    echo "[ERROR] 无法从 file 输出推断 DOCKER_PLATFORM: $info" >&2
    return 1
  fi
}

# 压缩 Linux ELF；Mach-O 请用本机 upx + --force-macos
upx_compress_in_docker() {
  local src="${1:?src binary required}"
  local dst="${2:-${src}.upx}"

  if [[ ! -f "$src" ]]; then
    echo "[ERROR] 未找到: $src" >&2
    return 1
  fi

  local info
  info="$(file -b "$src")"
  if [[ "$info" != *"ELF"* ]]; then
    echo "[ERROR] upx_compress_in_docker 仅支持 Linux ELF，当前: $info" >&2
    return 1
  fi

  local platform="${DOCKER_PLATFORM:-$(upx_docker_detect_platform "$info")}"
  local src_dir dst_dir src_base dst_base
  src_dir="$(cd "$(dirname "$src")" && pwd)"
  src_base="$(basename "$src")"
  dst_dir="$(cd "$(dirname "$dst")" && pwd)"
  dst_base="$(basename "$dst")"

  cp -f "$src" "$dst"
  chmod +x "$dst"

  upx_docker_ensure_image "$platform"

  read -r -a upx_args <<< "$(upx_build_args)"
  echo "==> UPX in Docker (${UPX_MODE:-best}, $platform): $dst"
  docker run --rm --platform "$platform" \
    -v "${src_dir}:/work:rw" \
    -w /work \
    "$UPX_DOCKER_IMAGE" \
    "${upx_args[@]}" "$dst_base"

  local raw_bytes upx_bytes ratio
  raw_bytes="$(wc -c < "$src" | tr -d ' ')"
  upx_bytes="$(wc -c < "$dst" | tr -d ' ')"
  ratio="$(awk "BEGIN { printf \"%.2f\", ($upx_bytes / $raw_bytes) * 100 }")"
  echo "raw:  $src ($(du -h "$src" | cut -f1))"
  echo "upx:  $dst ($(du -h "$dst" | cut -f1), ${ratio}%)"
}

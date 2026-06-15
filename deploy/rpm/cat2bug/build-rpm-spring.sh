#!/usr/bin/env bash
# 从 Spring Native 二进制生成 RPM（需 nfpm：https://nfpm.goreleaser.com/）
#
# 用法:
#   ./deploy/build-native-spring.sh x86_64
#   ./deploy/rpm/cat2bug/build-rpm-spring.sh x86_64
#   NFPM_VERSION=1.0.0 ./deploy/rpm/cat2bug/build-rpm-spring.sh aarch64
set -euo pipefail

if ! command -v nfpm >/dev/null 2>&1; then
  echo "nfpm is required to build RPM packages." >&2
  echo "  macOS:   brew install nfpm" >&2
  echo "  Linux:   go install github.com/goreleaser/nfpm/v2/cmd/nfpm@latest" >&2
  echo "  Docs:    https://nfpm.goreleaser.com/install/" >&2
  exit 1
fi

ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
ADMIN="$ROOT/cat2bug-platform-admin"
RPM_DIR="$(cd "$(dirname "$0")" && pwd)"
ARCH="${1:-$(uname -m)}"

case "$ARCH" in
  x86_64|amd64) GOARCH=amd64; RPM_ARCH=x86_64 ;;
  aarch64|arm64) GOARCH=arm64; RPM_ARCH=aarch64 ;;
  *) echo "Unsupported arch: $ARCH"; exit 1 ;;
esac

BIN="$ADMIN/target/cat2bug-admin-linux-${GOARCH}"
if [[ ! -f "$BIN" ]]; then
  BIN="$ADMIN/target/cat2bug-admin"
fi
if [[ ! -f "$BIN" ]] || [[ "$(file -b "$BIN")" != *"ELF"* ]]; then
  echo "Missing native ELF — run: ./deploy/build-native-spring.sh $ARCH" >&2
  exit 1
fi

VERSION="${NFPM_VERSION:-1.0.0}"
STAGE="$RPM_DIR/stage"
rm -rf "$STAGE"
mkdir -p "$STAGE/usr/bin" "$STAGE/etc/cat2bug" "$STAGE/lib/systemd/system"
cp "$BIN" "$STAGE/usr/bin/cat2bug-admin"
chmod 755 "$STAGE/usr/bin/cat2bug-admin"
cp "$RPM_DIR/cat2bug-platform.service" "$STAGE/lib/systemd/system/"
cp "$RPM_DIR/application.properties" "$STAGE/etc/cat2bug/platform.properties"

mkdir -p "$RPM_DIR/dist"
export NFPM_ARCH="$RPM_ARCH"
export NFPM_VERSION="$VERSION"
cd "$RPM_DIR"
nfpm package -f nfpm.yaml -t dist --packager rpm
BUILT="$(ls -1 dist/cat2bug-platform-*.${RPM_ARCH}.rpm | head -1)"
OUT="dist/cat2bug-platform_${VERSION}_${RPM_ARCH}.rpm"
mv -f "$BUILT" "$OUT"

echo "RPM: $RPM_DIR/$OUT"

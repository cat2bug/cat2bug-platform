#!/usr/bin/env bash
# 校验 readme/production/images 下 PNG 尺寸与可选单文件体积上限。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
IMAGES_DIR="${DOCS_IMAGES_DIR:-$ROOT/readme/production/images}"
MAX_WIDTH="${DOCS_IMAGES_MAX_WIDTH:-1600}"
MAX_FILE_BYTES="${DOCS_IMAGES_MAX_FILE_BYTES:-0}"

if [[ ! -d "$IMAGES_DIR" ]]; then
  echo "error: images dir not found: $IMAGES_DIR" >&2
  exit 1
fi

python3 - "$IMAGES_DIR" "$ROOT" "$MAX_WIDTH" "$MAX_FILE_BYTES" <<'PY'
import subprocess
import sys
from pathlib import Path

images_dir = Path(sys.argv[1])
root = Path(sys.argv[2])
max_width = int(sys.argv[3])
max_file_bytes = int(sys.argv[4])

def png_width(path: Path) -> int | None:
    try:
        out = subprocess.check_output(
            ["sips", "-g", "pixelWidth", str(path)], stderr=subprocess.DEVNULL, text=True
        )
        for line in out.splitlines():
            if "pixelWidth" in line:
                return int(line.split(":")[1].strip())
    except (FileNotFoundError, subprocess.CalledProcessError):
        pass
    try:
        out = subprocess.check_output(
            ["magick", "identify", "-format", "%w", str(path)],
            stderr=subprocess.DEVNULL,
            text=True,
        )
        return int(out.strip())
    except (FileNotFoundError, subprocess.CalledProcessError, ValueError):
        return None

violations = 0
warnings = 0
total_bytes = 0
png_count = 0

for path in sorted(images_dir.rglob("*.png")):
    png_count += 1
    size = path.stat().st_size
    total_bytes += size
    width = png_width(path)
    if width is None:
        print(f"warn: cannot read width: {path.relative_to(root)}", file=sys.stderr)
        warnings += 1
        continue
    if width > max_width:
        print(
            f"error: width {width}px > {max_width}px: {path.relative_to(root)}",
            file=sys.stderr,
        )
        violations += 1
    if max_file_bytes > 0 and size > max_file_bytes:
        print(
            f"warn: size {size} bytes > {max_file_bytes}: {path.relative_to(root)}",
            file=sys.stderr,
        )
        warnings += 1

mb = total_bytes / 1024 / 1024
print(f"check-docs-images: {png_count} PNG, total {total_bytes} bytes ({mb:.2f} MB)")

if violations:
    print(f"failed: {violations} width violation(s)", file=sys.stderr)
    sys.exit(1)
if warnings:
    print(f"passed with {warnings} warning(s)")
else:
    print("passed")
PY

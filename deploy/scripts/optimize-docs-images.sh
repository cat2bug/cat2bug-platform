#!/usr/bin/env bash
# 批量优化 readme/production/images PNG：只缩小超宽图 + pngquant + oxipng。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
IMAGES_DIR="${DOCS_IMAGES_DIR:-$ROOT/readme/production/images}"
MAX_WIDTH="${DOCS_IMAGES_MAX_WIDTH:-1600}"
PNGQUANT_QUALITY="${PNGQUANT_QUALITY:-65-90}"
DRY_RUN=0
REPORT_FILE=""

usage() {
  cat <<'EOF'
Usage: optimize-docs-images.sh [--dry-run] [--report FILE]

  --dry-run       只输出报告，不写回文件
  --report FILE   将摘要写入 FILE（Markdown）

依赖: magick (ImageMagick), pngquant, oxipng, sips
安装: brew install imagemagick pngquant oxipng
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run) DRY_RUN=1; shift ;;
    --report) REPORT_FILE="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "unknown arg: $1" >&2; usage; exit 1 ;;
  esac
done

for cmd in magick pngquant oxipng sips; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "error: required command not found: $cmd" >&2
    echo "  macOS: brew install imagemagick pngquant oxipng" >&2
    exit 1
  fi
done

if [[ ! -d "$IMAGES_DIR" ]]; then
  echo "error: images dir not found: $IMAGES_DIR" >&2
  exit 1
fi

before_total=0
after_total_est=0
processed=0
resized=0
quantized_smaller=0
unchanged=0
increased=0

report_lines=()
report_lines+=("# docs/images optimize report")
report_lines+=("")
report_lines+=("- dry-run: ${DRY_RUN}")
report_lines+=("- max width: ${MAX_WIDTH}")
report_lines+=("- pngquant quality: ${PNGQUANT_QUALITY}")
report_lines+=("")
report_lines+=("| file | dimensions | before B | after B | delta |")
report_lines+=("|------|------------|----------|---------|-------|")

format_mb() {
  python3 -c "b=$1; print(f'{b/1024/1024:.2f}')"
}

tmp_dir="$(mktemp -d)"
cleanup() { rm -rf "$tmp_dir"; }
trap cleanup EXIT

optimize_one() {
  local file="$1"
  local rel="${file#$ROOT/}"
  local width height
  width="$(sips -g pixelWidth "$file" | awk '/pixelWidth/ {print $2}')"
  height="$(sips -g pixelHeight "$file" | awk '/pixelHeight/ {print $2}')"
  local before_size
  before_size="$(stat -f%z "$file" 2>/dev/null || stat -c%s "$file")"
  before_total=$((before_total + before_size))

  local work="$tmp_dir/work.png"
  local quant="$tmp_dir/quant.png"
  local final="$tmp_dir/final.png"

  if (( width > MAX_WIDTH )); then
    magick "$file" -filter Lanczos -resize "${MAX_WIDTH}x" -strip "$work"
    resized=$((resized + 1))
  else
    cp "$file" "$work"
  fi

  if pngquant --quality="$PNGQUANT_QUALITY" --skip-if-larger --force --output "$quant" "$work" 2>/dev/null; then
    cp "$quant" "$final"
  else
    cp "$work" "$final"
  fi

  oxipng -o max --strip safe -q "$final" 2>/dev/null || true

  local after_size
  after_size="$(stat -f%z "$final" 2>/dev/null || stat -c%s "$final")"
  after_total_est=$((after_total_est + after_size))
  processed=$((processed + 1))

  local delta=$((after_size - before_size))
  if (( after_size < before_size * 95 / 100 )); then
    quantized_smaller=$((quantized_smaller + 1))
  elif (( after_size > before_size * 105 / 100 )); then
    increased=$((increased + 1))
    if (( DRY_RUN == 0 )); then
      # skip-if-larger 语义：若优化后更大则保留原文件
      after_total_est=$((after_total_est - after_size + before_size))
      unchanged=$((unchanged + 1))
      return 0
    fi
  else
    unchanged=$((unchanged + 1))
  fi

  if (( DRY_RUN == 0 )); then
    if (( after_size <= before_size )); then
      mv "$final" "$file"
    fi
  fi

  if (( width > MAX_WIDTH || delta < -1024 || delta > 1024 )); then
    report_lines+=("| \`${rel}\` | ${width}x${height} | ${before_size} | ${after_size} | ${delta} |")
  fi
}

while IFS= read -r -d '' file; do
  optimize_one "$file"
done < <(find "$IMAGES_DIR" -type f -iname '*.png' -print0 | sort -z)

saved=$((before_total - after_total_est))
report_summary=(
  ""
  "## Summary"
  ""
  "- PNG processed: ${processed}"
  "- Resized (w>${MAX_WIDTH}): ${resized}"
  "- Smaller after optimize: ${quantized_smaller}"
  "- Unchanged (~same): ${unchanged}"
  "- Skipped (would increase): ${increased}"
  "- Before: ${before_total} bytes ($(format_mb "$before_total") MB)"
  "- After (est): ${after_total_est} bytes ($(format_mb "$after_total_est") MB)"
  "- Delta: ${saved} bytes ($(format_mb "$saved") MB)"
)

printf '%s\n' "${report_lines[@]}" "${report_summary[@]}"

if [[ -n "$REPORT_FILE" ]]; then
  mkdir -p "$(dirname "$REPORT_FILE")"
  printf '%s\n' "${report_lines[@]}" "${report_summary[@]}" > "$REPORT_FILE"
  echo "report written: $REPORT_FILE"
fi

if (( DRY_RUN == 1 )); then
  echo "dry-run complete (no files modified)"
else
  echo "optimize complete"
fi

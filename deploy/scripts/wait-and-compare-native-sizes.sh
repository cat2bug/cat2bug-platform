#!/usr/bin/env bash
# 等待 rebuild-native-all 结束后执行 API-only 体积对比
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LOG="${LOG:-/tmp/cat2bug-api-only-compare.log}"
exec > >(tee -a "$LOG") 2>&1

echo "==> 等待 rebuild-native-all 结束..."
while pgrep -f 'rebuild-native-all.sh' >/dev/null 2>&1; do
  sleep 30
done
echo "==> rebuild 已结束，开始 API-only 对比"
"$ROOT/deploy/scripts/compare-native-sizes.sh"

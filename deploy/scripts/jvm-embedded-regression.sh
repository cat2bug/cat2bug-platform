#!/usr/bin/env bash
# JVM embedded 回归：admin 模块全量测试 + 打包（跳过测试）。
#
# 用法（在 cat2bug-platform 仓库根目录）:
#   ./deploy/scripts/jvm-embedded-regression.sh
#
# 日志: deploy/test/logs/jvm-regression-YYYYMMDD.log（追加写入）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
if [[ ! -f "$ROOT/pom.xml" ]]; then
  echo "[FAIL] 未找到 $ROOT/pom.xml" >&2
  exit 1
fi
cd "$ROOT"

LOG_DIR="$ROOT/deploy/test/logs"
mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/jvm-regression-$(date +%Y%m%d).log"

log_section() {
  {
    echo ""
    echo "======== $(date -Iseconds) $* ========"
  } | tee -a "$LOG_FILE"
}

run_cmd() {
  log_section "$*"
  set +e
  "$@" 2>&1 | tee -a "$LOG_FILE"
  local ec=${PIPESTATUS[0]}
  set -e
  echo "[exit=$ec] $*" >> "$LOG_FILE"
  return "$ec"
}

log_section "jvm-embedded-regression start host=$(uname -s)-$(uname -m) pwd=$ROOT"
# POI 相关单测（DefectCustomFieldExcelSupportTest）在部分受限环境可能 Abort trap；CI/本机请用完整权限执行。

FAIL=0
run_cmd mvn -pl cat2bug-platform-admin -am test || FAIL=1
run_cmd mvn -pl cat2bug-platform-admin -am package -DskipTests || FAIL=1

if [[ -f "$ROOT/cat2bug-platform-admin/target/cat2bug-admin.jar" ]]; then
  du -h "$ROOT/cat2bug-platform-admin/target/cat2bug-admin.jar" | tee -a "$LOG_FILE"
fi

log_section "jvm-embedded-regression done overall_fail=$FAIL log=$LOG_FILE"
exit "$FAIL"

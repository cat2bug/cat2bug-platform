#!/usr/bin/env bash
# H2 Mapper 冒烟：不打包 JAR，只编译相关模块并跑秒级单测。
#
# 必须在 cat2bug-platform 仓库根目录执行，例如:
#   cd /path/to/cat2bug-platform
#   ./deploy/scripts/h2-mapper-smoke.sh
#
# 也可从任意目录:
#   bash /path/to/cat2bug-platform/deploy/scripts/h2-mapper-smoke.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
if [[ ! -f "$ROOT/pom.xml" ]]; then
  echo "[FAIL] 未找到 $ROOT/pom.xml" >&2
  echo "请在本脚本所在仓库根目录 cat2bug-platform 下执行。" >&2
  exit 1
fi
cd "$ROOT"

MODE="${1:-test}"

compile_modules() {
  mvn -q -pl cat2bug-platform-framework,cat2bug-platform-system -am compile -DskipTests
}

run_mapper_test() {
  mvn -q -pl cat2bug-platform-system test -Dtest=H2MapperSmokeTest
}

case "$MODE" in
  compile)
    compile_modules
    echo "[OK] compile framework + system"
    ;;
  test)
    compile_modules
    run_mapper_test
    echo "[OK] H2MapperSmokeTest passed"
    ;;
  *)
    echo "Usage: $0 [test|compile]" >&2
    exit 1
    ;;
esac

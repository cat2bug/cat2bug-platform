#!/usr/bin/env bash
# 启动 Quarkus 单 JAR；若已安装则自动加载 config/install 运行期配置
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="${1:-$ROOT/cat2bug-platform-admin/target/cat2bug-admin.jar}"
shift || true

if [[ ! -f "$JAR" ]]; then
  echo "[ERROR] 找不到 JAR: $JAR" >&2
  echo "请先执行: ./deploy/build-embedded.sh" >&2
  exit 1
fi

WORKDIR="$(cd "$(dirname "$JAR")" && pwd)"
cd "$WORKDIR"

CONFIG_LOCATIONS=""
resolve_install_file() {
  local candidate
  for candidate in \
    "./config/install/application-install.yml" \
    "$WORKDIR/config/install/application-install.yml" \
    "$WORKDIR/../config/install/application-install.yml"; do
    if [[ -f "$candidate" ]]; then
      echo "$candidate"
      return 0
    fi
  done
  return 1
}
resolve_runtime_file() {
  local candidate
  for candidate in \
    "./config/install/quarkus-runtime.properties" \
    "$WORKDIR/config/install/quarkus-runtime.properties" \
    "$WORKDIR/../config/install/quarkus-runtime.properties"; do
    if [[ -f "$candidate" ]]; then
      echo "$candidate"
      return 0
    fi
  done
  return 1
}

if INSTALL_FILE="$(resolve_install_file)"; then
  CONFIG_LOCATIONS="$INSTALL_FILE"
fi
if RUNTIME_FILE="$(resolve_runtime_file)"; then
  if [[ -n "$CONFIG_LOCATIONS" ]]; then
    CONFIG_LOCATIONS="$CONFIG_LOCATIONS,$RUNTIME_FILE"
  else
    CONFIG_LOCATIONS="$RUNTIME_FILE"
  fi
fi

JAVA_OPTS_ARR=()
if [[ -n "${INSTALL_FILE:-}" ]]; then
  JAVA_OPTS_ARR+=("-Dcat2bug.install.config-path=$INSTALL_FILE")
fi
if [[ -n "$CONFIG_LOCATIONS" ]]; then
  exec java "-Dquarkus.config.locations=$CONFIG_LOCATIONS" ${JAVA_OPTS_ARR[@]+"${JAVA_OPTS_ARR[@]}"} -jar "$JAR" "$@"
fi

exec java -jar "$JAR" "$@"

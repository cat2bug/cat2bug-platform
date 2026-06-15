#!/usr/bin/env bash
# 对已停止的 H2 文件库执行 SQL（通过 Docker 挂载 volume，避免本机 Java 版本差异）
#
# 用法:
#   ./deploy/scripts/h2-exec.sh <docker-volume> "SELECT version FROM sys_db_version"
#   ./deploy/scripts/h2-exec.sh <docker-volume> @/path/to/script.sql
#
# 环境变量:
#   H2_DATABASE   库文件名（不含路径），默认 cat2bug_platform
#   H2_USER       默认 root
#   H2_PASSWORD   默认 cat2bug_password
#   H2_JAR        默认 ~/.m2/.../h2-2.3.230.jar
#   H2_DOCKER_IMG 默认 ghcr.io/graalvm/native-image-community:21-ol9
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

h2_default_jar() {
  local jar
  jar="${H2_JAR:-}"
  if [[ -n "$jar" && -f "$jar" ]]; then
    echo "$jar"
    return
  fi
  jar="$(ls -1 "${HOME}/.m2/repository/com/h2database/h2/"*/h2-*.jar 2>/dev/null | sort -V | tail -1 || true)"
  [[ -n "$jar" && -f "$jar" ]] || {
    echo "[ERROR] 未找到 H2 jar，请设置 H2_JAR 或先 mvn dependency:resolve" >&2
    exit 1
  }
  echo "$jar"
}

h2_jdbc_url() {
  local db="${H2_DATABASE:-cat2bug}"
  echo "jdbc:h2:file:/data/${db};MODE=MySQL;DATABASE_TO_LOWER=TRUE;"
}

# h2_exec_sql <volume> <sql-or-@file>
h2_exec_sql() {
  local vol="$1"
  local sql="$2"
  local jar img user pass url
  jar="$(h2_default_jar)"
  img="${H2_DOCKER_IMG:-ghcr.io/graalvm/native-image-community:21-ol9}"
  user="${H2_USER:-root}"
  pass="${H2_PASSWORD:-cat2bug_password}"
  url="$(h2_jdbc_url)"

  docker run --rm --entrypoint java --platform "${DOCKER_PLATFORM:-linux/arm64}" \
    -v "${vol}:/data" \
    -v "${jar}:/h2.jar:ro" \
    "$img" \
    -cp /h2.jar org.h2.tools.Shell \
    -url "$url" \
    -user "$user" \
    -password "$pass" \
    -sql "$sql"
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  [[ $# -ge 2 ]] || {
    echo "Usage: $0 <docker-volume> <sql>" >&2
    exit 1
  }
  h2_exec_sql "$1" "$2"
fi

#!/usr/bin/env bash
# 从仓库根目录启动 Quarkus 开发模式；安装完成后自动重启以加载运行期配置
set -euo pipefail

# Quarkus 模块需要 JDK 17+ 与 Maven >= 3.6.3（enforcer 插件要求）
setup_build_env() {
  if [[ -d /opt/homebrew/bin ]]; then
    export PATH="/opt/homebrew/bin:$PATH"
  elif [[ -d /usr/local/bin ]]; then
    export PATH="/usr/local/bin:$PATH"
  fi

  if [[ -z "${JAVA_HOME:-}" ]] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -qE 'version "(1[7-9]|[2-9][0-9])'; then
    if command -v jenv >/dev/null 2>&1 && jenv prefix 17 >/dev/null 2>&1; then
      export JAVA_HOME="$(jenv prefix 17)"
    elif [[ -d /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ]]; then
      export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
    else
      echo "[ERROR] 需要 JDK 17。示例: jenv local 17 或 brew install openjdk@17" >&2
      exit 1
    fi
    export PATH="$JAVA_HOME/bin:$PATH"
  fi

  local mvn_ver
  mvn_ver="$(mvn -version 2>/dev/null | awk '/Apache Maven/{print $3}')"
  if [[ -z "$mvn_ver" ]] || [[ "$(printf '%s\n' "3.6.3" "$mvn_ver" | sort -V | head -1)" != "3.6.3" ]]; then
    echo "[ERROR] 需要 Maven >= 3.6.3（当前: ${mvn_ver:-未知}）。示例: brew install maven" >&2
    exit 1
  fi
}

resolve_install_config() {
  RUNTIME_PROPS=""
  for candidate in \
    "$ROOT/config/install/quarkus-runtime.properties" \
    "$ROOT/cat2bug-platform-quarkus-admin/config/install/quarkus-runtime.properties"
  do
    if [[ -f "$candidate" ]]; then
      RUNTIME_PROPS="$candidate"
      break
    fi
  done

  INSTALL_YML=""
  for candidate in \
    "$ROOT/config/install/application-install.yml" \
    "$ROOT/cat2bug-platform-quarkus-admin/config/install/application-install.yml"
  do
    if [[ -f "$candidate" ]]; then
      INSTALL_YML="$candidate"
      break
    fi
  done

  PENDING_RESTART=""
  for candidate in \
    "$ROOT/config/install/.cat2bug-pending-restart" \
    "$ROOT/cat2bug-platform-quarkus-admin/config/install/.cat2bug-pending-restart"
  do
    if [[ -f "$candidate" ]]; then
      PENDING_RESTART="$candidate"
      break
    fi
  done

  CONFIG_LOCATIONS=""
  if [[ -n "$INSTALL_YML" ]]; then
    CONFIG_LOCATIONS="$INSTALL_YML"
  fi
  if [[ -n "$RUNTIME_PROPS" ]]; then
    if [[ -n "$CONFIG_LOCATIONS" ]]; then
      CONFIG_LOCATIONS="$CONFIG_LOCATIONS,$RUNTIME_PROPS"
    else
      CONFIG_LOCATIONS="$RUNTIME_PROPS"
    fi
  fi
  if [[ -n "$CONFIG_LOCATIONS" ]]; then
    export QUARKUS_CONFIG_LOCATIONS="$CONFIG_LOCATIONS"
  else
    unset QUARKUS_CONFIG_LOCATIONS || true
  fi
}

setup_build_env

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
ADMIN_POM="$ROOT/cat2bug-platform-quarkus-admin/pom.xml"

# install 阶段不加载 install 运行期配置（避免 build 期按 MySQL 解析但缺驱动）
mvn -Pquarkus -pl cat2bug-platform-quarkus-admin -am install -DskipTests -q

while true; do
  resolve_install_config

  mvn -Pquarkus -f "$ADMIN_POM" quarkus:dev "$@"
  exit_code=$?

  if [[ $exit_code -ne 0 ]]; then
    exit "$exit_code"
  fi

  if [[ -n "$PENDING_RESTART" ]]; then
    echo "[INFO] 安装向导已完成，正在重新启动以加载运行期配置..."
    sleep 2
    continue
  fi
  break
done

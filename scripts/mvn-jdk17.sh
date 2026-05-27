#!/usr/bin/env bash
# 使用 JDK 17 调用 Maven（避免系统默认 Java 11 导致 release 17 编译失败）
set -euo pipefail

resolve_java_home() {
  if [[ -n "${JAVA_HOME:-}" ]] && "${JAVA_HOME}/bin/java" -version 2>&1 | grep -qE 'version "17'; then
    return 0
  fi
  if [[ -d /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ]]; then
    JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
    export JAVA_HOME
    return 0
  fi
  if [[ -d /usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ]]; then
    JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
    export JAVA_HOME
    return 0
  fi
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    local jh
    jh=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
    if [[ -n "$jh" ]]; then
      JAVA_HOME="$jh"
      export JAVA_HOME
      return 0
    fi
  fi
  return 1
}

if ! resolve_java_home; then
  echo "错误: 未找到 JDK 17。请先安装，例如: brew install openjdk@17" >&2
  exit 1
fi

export PATH="$JAVA_HOME/bin:$PATH"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
exec mvn "$@"

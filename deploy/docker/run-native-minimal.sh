#!/usr/bin/env bash
# OpenSpec 兼容入口：委托 Spring Native 最小 Docker 运行脚本
# 等价于 deploy/docker/run-native-spring-minimal.sh
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
exec "$ROOT/deploy/docker/run-native-spring-minimal.sh" "$@"

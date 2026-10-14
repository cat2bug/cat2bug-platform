#!/usr/bin/env bash
# Docker Compose 镜像构建前置：前端 dist（nginx 容器）+ 后端 admin uber-jar（after 容器）
#
# 用法:
#   ./deploy/docker/build.sh
#   SKIP_FRONTEND=true ./deploy/docker/build.sh   # 只重打后端 JAR
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DOCKER_DIR="$(cd "$(dirname "$0")" && pwd)"
ADMIN="$ROOT/cat2bug-platform-admin"

if [[ "${SKIP_FRONTEND:-false}" != "true" ]]; then
  echo "==> 前端 build:prod"
  (cd "$ROOT/cat2bug-platform-ui" && NODE_OPTIONS="${NODE_OPTIONS:---openssl-legacy-provider}" npm run build:prod)

  echo "==> 拷贝 dist -> deploy/docker/static"
  rm -rf "$DOCKER_DIR/static"
  cp -R "$ROOT/cat2bug-platform-ui/dist" "$DOCKER_DIR/static"
fi

# Compose 固定使用 MySQL；命名池 db-kind 与 MyBatis database-id 为构建期属性，须按 mysql profile 打包
echo "==> 后端 mvn -Prelease -Dquarkus.profile=mysql -pl cat2bug-platform-admin -am package -DskipTests"
rm -f "$ADMIN/target/cat2bug-admin-runner.jar"
(cd "$ROOT" && mvn -Prelease -Dquarkus.profile=mysql -pl cat2bug-platform-admin -am package -DskipTests)

JAR="$ADMIN/target/cat2bug-admin-runner.jar"
if [[ ! -f "$JAR" ]]; then
  echo "[ERROR] 未找到 uber-jar: $JAR" >&2
  exit 1
fi

echo "==> 拷贝 $(basename "$JAR") -> deploy/docker/cat2bug-admin.jar"
cp -f "$JAR" "$DOCKER_DIR/cat2bug-admin.jar"

echo "Done. 在 deploy/docker 目录执行: docker compose build && docker compose up -d"

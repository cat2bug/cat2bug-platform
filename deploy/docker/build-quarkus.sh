#!/usr/bin/env bash
# Docker Compose 镜像构建前置：前端 dist + Quarkus api-only 后端 JAR（替代 Spring）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DOCKER_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "==> 前端 build:prod"
(cd "$ROOT/cat2bug-platform-ui" && NODE_OPTIONS="${NODE_OPTIONS:---openssl-legacy-provider}" npm run build:prod)

echo "==> 拷贝 dist -> deploy/docker/static"
rm -rf "$DOCKER_DIR/static"
cp -R "$ROOT/cat2bug-platform-ui/dist" "$DOCKER_DIR/static"

echo "==> 后端 mvn -Pquarkus -Prelease -pl cat2bug-platform-quarkus-admin -am package -DskipTests"
(cd "$ROOT" && mvn -Pquarkus -Prelease -pl cat2bug-platform-quarkus-admin -am package -DskipTests)

JAR="$ROOT/cat2bug-platform-quarkus-admin/target/cat2bug-admin-runner.jar"
if [[ ! -f "$JAR" ]]; then
  JAR="$ROOT/cat2bug-platform-quarkus-admin/target/cat2bug-admin.jar"
fi
if [[ ! -f "$JAR" ]]; then
  JAR="$ROOT/cat2bug-platform-quarkus-admin/target/quarkus-app/quarkus-run.jar"
fi

echo "==> 拷贝 cat2bug-admin.jar (Quarkus)"
cp "$JAR" "$DOCKER_DIR/cat2bug-admin.jar"

echo "Done. 在 deploy/docker 目录执行: docker-compose build && docker-compose up -d"

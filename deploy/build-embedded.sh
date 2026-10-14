#!/usr/bin/env bash
# Quarkus 单 JAR Release：embedded 前端 + embedded profile 后端
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> 前端 build:embedded"
(cd "$ROOT/cat2bug-platform-ui" && NODE_OPTIONS="${NODE_OPTIONS:---openssl-legacy-provider}" npm run build:embedded)

echo "==> 后端 mvn -Pembedded -Prelease -pl cat2bug-platform-admin -am package -DskipTests"
# Maven 增量复制不会删除旧 hash 的前端文件，残留会被一并打进 JAR
rm -rf "$ROOT/cat2bug-platform-admin/target/classes/META-INF/resources"
(cd "$ROOT" && mvn -Pembedded -Prelease -pl cat2bug-platform-admin -am package -DskipTests)

JAR="$ROOT/cat2bug-platform-admin/target/cat2bug-admin-runner.jar"
if [[ ! -f "$JAR" ]]; then
  JAR="$ROOT/cat2bug-platform-admin/target/cat2bug-admin.jar"
fi
if [[ ! -f "$JAR" ]]; then
  JAR="$ROOT/cat2bug-platform-admin/target/quarkus-app/quarkus-run.jar"
fi
if [[ ! -f "$JAR" ]]; then
  JAR="$ROOT/cat2bug-platform-admin/target/cat2bug-admin.jar"
fi
if [[ -f "$JAR" ]]; then
  SIZE="$(du -h "$JAR" | cut -f1)"
  OUT="$ROOT/cat2bug-platform-admin/target/cat2bug-admin.jar"
  if [[ "$JAR" != "$OUT" ]]; then
    cp -f "$JAR" "$OUT"
    echo "Also: $OUT (copy of $(basename "$JAR"))"
  fi
  echo "Done: $OUT ($SIZE)"
else
  echo "Done: see cat2bug-platform-admin/target/"
fi

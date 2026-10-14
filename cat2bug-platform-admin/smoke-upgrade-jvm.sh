#!/usr/bin/env bash
# 升级冒烟：用旧版单 JAR 完成安装并登录，再换成新版 JAR 在同一目录启动，确认旧安装配置与数据可直接沿用。
#
# 用法：
#   OLD_JAR=/path/old.jar NEW_JAR=/path/new.jar ./smoke-upgrade-jvm.sh h2
#   OLD_JAR=... NEW_JAR=... SMOKE_MYSQL_PASSWORD=*** ./smoke-upgrade-jvm.sh mysql
# MySQL 可选变量：SMOKE_MYSQL_HOST(127.0.0.1) SMOKE_MYSQL_PORT(3306) SMOKE_MYSQL_USER(root)
#   SMOKE_MYSQL_CONTAINER：给出 Docker 容器名时，结束后用它删除临时库
set -euo pipefail

ADMIN_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$ADMIN_DIR/.." && pwd)"
DB="${1:-h2}"
OLD_JAR="${OLD_JAR:?OLD_JAR 未设置}"
NEW_JAR="${NEW_JAR:?NEW_JAR 未设置}"
PORT="${SMOKE_PORT:-2031}"
BASE="http://127.0.0.1:$PORT"
WORK="${SMOKE_WORK:-/tmp/cat2bug-upgrade-smoke-$DB}"
MYSQL_DB="cat2bug_upgrade_smoke_$(date +%s)"

APP_PID=""
stop_app() {
  if [[ -n "$APP_PID" ]] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID" 2>/dev/null || true
    for _ in $(seq 1 30); do kill -0 "$APP_PID" 2>/dev/null || break; sleep 1; done
    kill -9 "$APP_PID" 2>/dev/null || true
  fi
  APP_PID=""
  for _ in $(seq 1 30); do
    lsof -iTCP:"$PORT" -sTCP:LISTEN -n >/dev/null 2>&1 || return 0
    sleep 1
  done
  echo "端口 $PORT 仍被占用"; exit 1
}
cleanup() {
  stop_app
  if [[ "$DB" == "mysql" && -n "${SMOKE_MYSQL_CONTAINER:-}" ]]; then
    docker exec -e MYSQL_PWD="$SMOKE_MYSQL_PASSWORD" "$SMOKE_MYSQL_CONTAINER" \
      mysql -u"${SMOKE_MYSQL_USER:-root}" -e "DROP DATABASE IF EXISTS \`$MYSQL_DB\`" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

start_app() { # label
  if lsof -iTCP:"$PORT" -sTCP:LISTEN -n >/dev/null 2>&1; then
    echo "端口 $PORT 已被占用"; exit 1
  fi
  # exec 让 APP_PID 就是 java 进程，否则 kill 只结束外层 shell
  (cd "$WORK" && exec env CAT2BUG_DISABLE_SELF_RESTART=true QUARKUS_HTTP_PORT="$PORT" \
    "$ROOT/deploy/run-jar.sh" "$WORK/app.jar" >"$WORK/$1.log" 2>&1) &
  APP_PID=$!
  for i in $(seq 1 120); do
    if curl -sf "$BASE/q/health/ready" >/dev/null 2>&1; then
      echo "[$1] health: OK (${i}s)"
      return 0
    fi
    sleep 1
  done
  echo "[$1] health: TIMEOUT"; tail -60 "$WORK/$1.log"; exit 1
}

login() { # label -> 打印 token
  local body token
  body=$(curl -s -X POST "$BASE/login" -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"cat2bug"}' || true)
  token=$(echo "$body" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
  if [[ -z "$token" ]]; then
    echo "[$1] login: FAIL $(echo "$body" | head -c 200)" >&2; exit 1
  fi
  echo "$token"
}

fingerprint() { # token -> 用于新旧版本比对的数据摘要
  local info tid
  info=$(curl -s "$BASE/getInfo" -H "Authorization: Bearer $1")
  tid=$(echo "$info" | grep -o '"teamId":[0-9]*' | head -1 | cut -d: -f2 || true)
  echo "user=$(echo "$info" | grep -o '"userName":"[^"]*"' | head -1) teamId=${tid:-none}" \
    "projects=$(total "$1" "/system/project/list?teamId=${tid:-1}&pageNum=1&pageSize=50")" \
    "users=$(total "$1" "/system/user/list?pageNum=1&pageSize=50")" \
    "roles=$(total "$1" "/system/role/list?pageNum=1&pageSize=50")" \
    "dicts=$(total "$1" "/system/dict/type/list?pageNum=1&pageSize=50")"
}

total() { # token url -> total 字段，缺失时标记 FAIL
  local v
  v=$(curl -s "$BASE$2" -H "Authorization: Bearer $1" | grep -o '"total":[0-9]*' | head -1 | cut -d: -f2)
  echo "${v:-FAIL}"
}

rm -rf "$WORK" && mkdir -p "$WORK"
cp "$OLD_JAR" "$WORK/app.jar"

echo "==> 旧版本全新安装（${DB}）"
start_app old-setup
if [[ "$DB" == "mysql" ]]; then
  SUBMIT_DB=$(printf '"databaseType":"mysql","databaseMode":"new","mysqlHost":"%s","mysqlPort":%s,"mysqlDatabase":"%s","mysqlUsername":"%s","mysqlPassword":"%s"' \
    "${SMOKE_MYSQL_HOST:-127.0.0.1}" "${SMOKE_MYSQL_PORT:-3306}" "$MYSQL_DB" \
    "${SMOKE_MYSQL_USER:-root}" "${SMOKE_MYSQL_PASSWORD:?SMOKE_MYSQL_PASSWORD 未设置}")
else
  SUBMIT_DB='"databaseType":"h2","databaseMode":"new","h2Database":"cat2bug_platform"'
fi
SUBMIT=$(curl -s -X POST "$BASE/setup/submit" -H 'Content-Type: application/json' -d "{
  $SUBMIT_DB,\"cacheType\":\"local\",
  \"profile\":\"$WORK/upload\",\"temp\":\"$WORK/uploadTemp\",\"logPath\":\"$WORK/logs\",
  \"registerUser\":false,\"captchaEnabled\":false,\"aiEnabled\":false,
  \"adminUsername\":\"admin\",\"adminPassword\":\"cat2bug\"}" || true)
if ! echo "$SUBMIT" | grep -q '"code":200'; then
  echo "setup submit: FAIL $(echo "$SUBMIT" | head -c 300)"; tail -60 "$WORK/old-setup.log"; exit 1
fi
echo "setup submit: OK"
sleep 2
stop_app
RUNTIME="$WORK/config/install/quarkus-runtime.properties"
[[ -f "$RUNTIME" ]] || { echo "缺少 $RUNTIME"; exit 1; }
RUNTIME_SUM_BEFORE=$(shasum "$RUNTIME" | cut -d' ' -f1)

echo "==> 旧版本重启后登录"
start_app old-run
OLD_FP=$(fingerprint "$(login old-run)")
echo "[old-run] $OLD_FP"
stop_app

echo "==> 换成新版本，原目录启动"
cp "$NEW_JAR" "$WORK/app.jar"
start_app new-run
NEW_FP=$(fingerprint "$(login new-run)")
echo "[new-run] $NEW_FP"
STATUS=$(curl -s "$BASE/setup/status")
echo "$STATUS" | grep -q '"installed":true' || { echo "setup status: FAIL $(echo "$STATUS" | head -c 200)"; exit 1; }
echo "setup status: installed"
stop_app

[[ "$OLD_FP" != *FAIL* && "$NEW_FP" != *FAIL* ]] || { echo "列表查询失败"; exit 1; }
[[ "$OLD_FP" == "$NEW_FP" ]] || { echo "数据摘要不一致: old=[$OLD_FP] new=[$NEW_FP]"; exit 1; }
[[ "$(shasum "$RUNTIME" | cut -d' ' -f1)" == "$RUNTIME_SUM_BEFORE" ]] || { echo "运行期配置被改写"; exit 1; }
echo "Upgrade smoke (${DB}): OK"

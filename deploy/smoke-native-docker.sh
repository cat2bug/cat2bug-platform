#!/usr/bin/env bash
# 在 Docker 中冒烟 Linux Native 二进制（本机 macOS 无法直接跑 ELF）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ADMIN_DIR="$ROOT/cat2bug-platform-admin"
ARCH="${1:-arm64}"
USE_UPX="${USE_UPX:-true}"
case "$ARCH" in
  amd64|x86_64) GOARCH=amd64; DOCKER_PLATFORM=linux/amd64 ;;
  arm64|aarch64) GOARCH=arm64; DOCKER_PLATFORM=linux/arm64 ;;
  *) echo "Unsupported arch: $ARCH"; exit 1 ;;
esac

BIN="$ADMIN_DIR/target/cat2bug-admin-linux-${GOARCH}"
if [[ "$USE_UPX" == "true" && -f "${BIN}.upx" ]]; then
  BIN="${BIN}.upx"
fi
if [[ ! -f "$BIN" ]]; then
  echo "Missing $BIN — run ./deploy/build-native.sh ${ARCH} first"
  exit 1
fi

# 避免与本机 JVM :2020 冲突
pkill -f 'quarkus-run.jar' 2>/dev/null || true
pkill -f 'cat2bug-admin-linux' 2>/dev/null || true
sleep 1

DATA_DIR="${NATIVE_SMOKE_DATA:-/tmp/cat2bug-native-smoke}"
mkdir -p "$DATA_DIR"
# 复用 Gate A smoke 安装配置（若存在）
INSTALL_YML="${NATIVE_SMOKE_INSTALL:-/tmp/cat2bug-smoke/application-install.yml}"
RUNTIME_PROPS="${NATIVE_SMOKE_RUNTIME:-/tmp/cat2bug-smoke/quarkus-runtime.properties}"

CONTAINER="cat2bug-native-smoke-$$"
cleanup() {
  if [[ "${KEEP_CONTAINER:-false}" == "true" ]]; then
    echo "==> container kept: $CONTAINER"
    return
  fi
  docker rm -f "$CONTAINER" >/dev/null 2>&1 || true
}
trap cleanup EXIT

DOCKER_ARGS=(
  run -d --name "$CONTAINER" --platform "$DOCKER_PLATFORM"
  -p 2020:2020
  -v "$BIN:/usr/local/bin/cat2bug-admin:ro"
  -v "$DATA_DIR:/data"
  -w /data
)
FRESH="${FRESH:-false}"
if [[ "$FRESH" == "true" ]]; then
  # 全新安装：空数据目录，走 /setup/submit（H2）+ Flyway 迁移
  rm -rf "$DATA_DIR" && mkdir -p "$DATA_DIR"
elif [[ -f "$INSTALL_YML" && -f "$RUNTIME_PROPS" ]]; then
  # 宿主机路径在容器内不可见：复制 H2 库与上传目录到 DATA_DIR，并把配置里的路径改写为 /data
  # 须清空：FRESH 运行留下的 /data/config/install 会覆盖这里注入的配置
  rm -rf "$DATA_DIR"
  mkdir -p "$DATA_DIR/config" "$DATA_DIR/db"
  # install yml 里的 H2 地址优先于 runtime properties，须一并指向复制进容器的库
  sed -E 's#(url: *jdbc:h2:file:)[^;]*#\1/data/db/smoke_h2#' \
    "$INSTALL_YML" > "$DATA_DIR/config/application-install.yml"
  H2_FILE=$(sed -n 's/^quarkus\.datasource\.runtime-mysql\.jdbc\.url=jdbc:h2:file:\([^;]*\);.*/\1/p' "$RUNTIME_PROPS")
  if [[ -n "$H2_FILE" && -f "${H2_FILE}.mv.db" ]]; then
    cp "${H2_FILE}.mv.db" "$DATA_DIR/db/smoke_h2.mv.db"
  fi
  PROFILE_DIR=$(sed -n 's/^cat2bug\.profile=//p' "$RUNTIME_PROPS")
  if [[ -n "$PROFILE_DIR" && -d "$PROFILE_DIR" ]]; then
    rm -rf "$DATA_DIR/upload" && cp -R "$PROFILE_DIR" "$DATA_DIR/upload"
  fi
  sed -E \
    -e 's#^(quarkus\.datasource\.runtime-mysql\.jdbc\.url=jdbc:h2:file:)[^;]*#\1/data/db/smoke_h2#' \
    -e 's#^cat2bug\.profile=.*#cat2bug.profile=/data/upload#' \
    -e 's#^cat2bug\.temp=.*#cat2bug.temp=/data/uploadTemp#' \
    "$RUNTIME_PROPS" > "$DATA_DIR/config/quarkus-runtime.properties"
  DOCKER_ARGS+=(-e QUARKUS_CONFIG_LOCATIONS=file:///data/config/application-install.yml,file:///data/config/quarkus-runtime.properties)
  DOCKER_ARGS+=(-e CAT2BUG_INSTALL_CONFIG_PATH=/data/config/application-install.yml)
fi
DOCKER_ARGS+=(
  registry.access.redhat.com/ubi9/ubi-minimal:9.5
  /usr/local/bin/cat2bug-admin
)

docker "${DOCKER_ARGS[@]}"

wait_health() {
  echo "==> waiting for health..."
  for i in $(seq 1 90); do
    if curl -sf http://127.0.0.1:2020/q/health/ready >/dev/null 2>&1; then
      echo "health: OK (${i} checks)"
      return 0
    fi
    sleep 1
  done
  echo "health: TIMEOUT"
  docker logs "$CONTAINER" 2>&1 | tail -80
  exit 1
}
wait_health

if [[ "$FRESH" == "true" ]]; then
  STATUS=$(curl -s http://127.0.0.1:2020/setup/status || true)
  echo "setup status: $(echo "$STATUS" | head -c 160)"
  PREFLIGHT=$(curl -s http://127.0.0.1:2020/setup/preflight || true)
  if echo "$PREFLIGHT" | grep -q '"code":200'; then
    echo "setup preflight: OK"
  else
    echo "setup preflight: FAIL $(echo "$PREFLIGHT" | head -c 300)"
    exit 1
  fi
  SUBMIT=$(curl -s -X POST http://127.0.0.1:2020/setup/submit -H 'Content-Type: application/json' -d '{
    "databaseType":"h2","databaseMode":"new","h2Database":"cat2bug_platform","cacheType":"local",
    "profile":"/data/upload","temp":"/data/uploadTemp","logPath":"/data/logs",
    "registerUser":false,"captchaEnabled":false,"aiEnabled":false,
    "adminUsername":"admin","adminPassword":"cat2bug"}' || true)
  if ! echo "$SUBMIT" | grep -q '"code":200'; then
    echo "setup submit: FAIL $(echo "$SUBMIT" | head -c 300)"
    docker logs "$CONTAINER" 2>&1 | grep -iE 'error|exception' -A3 | tail -40
    exit 1
  fi
  echo "setup submit: OK $(echo "$SUBMIT" | head -c 200)"
  if echo "$SUBMIT" | grep -q '"restartRequired":true'; then
    echo "==> restart required, restarting container"
    docker restart "$CONTAINER" >/dev/null
    sleep 2
    wait_health
  else
    sleep 3
  fi
else
  # 复制来的库若落后于当前迁移脚本，先走升级流程（与用户升级路径一致）
  UPGRADE_STATUS=$(curl -s http://127.0.0.1:2020/upgrade/status || true)
  if echo "$UPGRADE_STATUS" | grep -q '"upgradeRequired":true'; then
    UPGRADE=$(curl -s -X POST http://127.0.0.1:2020/upgrade/submit -H 'Content-Type: application/json' \
      -d '{"adminUsername":"admin","adminPassword":"cat2bug","backupEnabled":false,"applyConfigChanges":false}' || true)
    if ! echo "$UPGRADE" | grep -q '"code":200'; then
      echo "upgrade submit: FAIL $(echo "$UPGRADE" | head -c 300)"
      docker logs "$CONTAINER" 2>&1 | grep -iE 'error|exception|upgrade|flyway' -A3 | tail -40
      exit 1
    fi
    echo "upgrade submit: OK"
    docker restart "$CONTAINER" >/dev/null
    sleep 2
    wait_health
  fi
fi

# SPA index
CODE=$(curl -s -o /tmp/native-index.html -w '%{http_code}' -H 'Accept: text/html' http://127.0.0.1:2020/)
if [[ "$CODE" == "200" ]] && grep -qiE '<html|app-container|id="app"' /tmp/native-index.html; then
  echo "spa: OK (HTTP $CODE)"
else
  echo "spa: FAIL (HTTP $CODE) $(head -c 120 /tmp/native-index.html | tr '\n' ' ')"
  exit 1
fi

VER=$(curl -sf http://127.0.0.1:2020/version || true)
[[ -n "$VER" ]] && echo "version: OK ($VER)" || { echo "version: FAIL"; exit 1; }

LOGIN=$(curl -sf -X POST http://127.0.0.1:2020/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"cat2bug"}' || true)
TOKEN=$(echo "$LOGIN" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
if [[ -z "$TOKEN" ]]; then
  echo "login: FAIL ($LOGIN)"
  # 新库可能无账号：仅健康+SPA 算部分通过
  echo "Smoke partial (no login user in Native data dir)"
  exit 0
fi
echo "login: OK"

FAILS=0
check() { # name url [method]
  local name="$1" url="$2" method="${3:-GET}" body
  body=$(curl -s -X "$method" "http://127.0.0.1:2020$url" -H "Authorization: Bearer $TOKEN" || true)
  if echo "$body" | grep -q '"code":200'; then
    echo "$name: OK"
  else
    echo "$name: FAIL $(echo "$body" | head -c 200)"
    FAILS=$((FAILS+1))
  fi
}

INFO=$(curl -s "http://127.0.0.1:2020/getInfo" -H "Authorization: Bearer $TOKEN" || true)
TID=$(echo "$INFO" | grep -o '"teamId":[0-9]*' | head -1 | cut -d: -f2 || true)
TID="${TID:-1}"
PROJECTS=$(curl -s "http://127.0.0.1:2020/system/project/list?teamId=$TID&pageNum=1&pageSize=5" -H "Authorization: Bearer $TOKEN" || true)
PID=$(echo "$PROJECTS" | grep -o '"projectId":[0-9]*' | head -1 | cut -d: -f2 || true)
if [[ -z "$PID" ]]; then
  # 种子库没有项目：建一个冒烟用团队与项目（创建者自动成为成员）
  TEAM=$(curl -s -X POST "http://127.0.0.1:2020/system/team" -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' -d '{"teamName":"冒烟团队","teamKey":"smoke"}' || true)
  NEW_TID=$(echo "$TEAM" | grep -o '"teamId":[0-9]*' | head -1 | cut -d: -f2 || true)
  if [[ -n "$NEW_TID" ]]; then
    TID="$NEW_TID"
    curl -s -X POST "http://127.0.0.1:2020/system/project" -H "Authorization: Bearer $TOKEN" \
      -H 'Content-Type: application/json' \
      -d "{\"teamId\":$TID,\"projectName\":\"冒烟项目\",\"projectKey\":\"smoke\"}" >/dev/null || true
    PROJECTS=$(curl -s "http://127.0.0.1:2020/system/project/list?teamId=$TID&pageNum=1&pageSize=5" -H "Authorization: Bearer $TOKEN" || true)
    PID=$(echo "$PROJECTS" | grep -o '"projectId":[0-9]*' | head -1 | cut -d: -f2 || true)
  fi
fi
HAS_PROJECT=true
[[ -z "$PID" ]] && HAS_PROJECT=false
PID="${PID:-1}"
echo "teamId=$TID projectId=$PID"
if [[ "$HAS_PROJECT" == "true" ]]; then
  # 种子库里 admin 不是项目成员、也没有当前项目：导入模板与 MCP 都依赖这两项
  curl -s -X POST "http://127.0.0.1:2020/system/project/$PID/member" -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' -d '{"memberIds":1}' >/dev/null || true
  curl -s -X PUT "http://127.0.0.1:2020/system/user-config" -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' -d "{\"currentProjectId\":$PID}" >/dev/null || true
  LOGIN=$(curl -sf -X POST http://127.0.0.1:2020/login -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"cat2bug"}' || true)
  TOKEN=$(echo "$LOGIN" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
  CASES=$(curl -s "http://127.0.0.1:2020/system/case/list?projectId=$PID&pageNum=1&pageSize=1" -H "Authorization: Bearer $TOKEN" || true)
  if echo "$CASES" | grep -q '"total":0'; then
    # 用例导出为空时导入检查无意义：先造一个模块和一条用例
    MOD=$(curl -s -X POST "http://127.0.0.1:2020/system/module" -H "Authorization: Bearer $TOKEN" \
      -H 'Content-Type: application/json' -d "{\"projectId\":$PID,\"modulePid\":0,\"moduleName\":\"冒烟模块\"}" || true)
    MOD_ID=$(echo "$MOD" | grep -o '"moduleId":[0-9]*' | head -1 | cut -d: -f2 || true)
    [[ -n "$MOD_ID" ]] && curl -s -X POST "http://127.0.0.1:2020/system/case/batch" -H "Authorization: Bearer $TOKEN" \
      -H 'Content-Type: application/json' \
      -d "[{\"projectId\":$PID,\"moduleId\":$MOD_ID,\"caseName\":\"冒烟用例\",\"caseType\":1,\"caseLevel\":1}]" >/dev/null || true
  fi
fi

check "user info"     "/getInfo"
check "routers"       "/getRouters"
check "project list"  "/system/project/list?teamId=$TID&pageNum=1&pageSize=5"
check "defect list"   "/system/defect/list?projectId=$PID&pageNum=1&pageSize=5"
check "case list"     "/system/case/list?projectId=$PID&pageNum=1&pageSize=5"
check "module list"   "/system/module/list?projectId=$PID"
check "plan list"     "/system/plan/list?projectId=$PID&pageNum=1&pageSize=5"
check "defect sort"   "/system/defect/list?projectId=$PID&pageNum=1&pageSize=5&orderByColumn=createMember&isAsc=descending"
check "case sort"     "/system/case/list?projectId=$PID&pageNum=1&pageSize=5&orderByColumn=caseLevel&isAsc=ascending"
check "oper log sort" "/monitor/operlog/list?pageNum=1&pageSize=5&orderByColumn=operTime&isAsc=descending"
SORT_BAD=$(curl -s "http://127.0.0.1:2020/system/defect/list?projectId=$PID&orderByColumn=defect_id%27" \
  -H "Authorization: Bearer $TOKEN" || true)
if echo "$SORT_BAD" | grep -q '参数不符合规范'; then
  echo "sort injection guard: OK"
else
  echo "sort injection guard: FAIL $(echo "$SORT_BAD" | head -c 200)"
  FAILS=$((FAILS+1))
fi
check "notice list"   "/system/notice/list?pageNum=1&pageSize=5"
if [[ "$HAS_PROJECT" == "true" ]]; then
  check "git list"      "/system/git/repo/list?projectId=$PID&pageNum=1&pageSize=5"
else
  echo "git list: SKIP (fresh DB has no project)"
fi

check "online users"  "/monitor/online/list"
check "server info"   "/monitor/server"
check "cache names"   "/monitor/cache/getNames"
check "oper log"      "/monitor/operlog/list?pageNum=1&pageSize=5"
check "login log"     "/monitor/logininfor/list?pageNum=1&pageSize=5"
check "job list"      "/monitor/job/list?pageNum=1&pageSize=5"
check "job log list"  "/monitor/jobLog/list?pageNum=1&pageSize=5"
DRUID_CODE=$(curl -s -o /tmp/native-druid.html -w '%{http_code}' -b "Admin-Token=$TOKEN" \
  http://127.0.0.1:2020/druid/login.html || true)
if [[ "$DRUID_CODE" == "200" ]] && grep -q 'Agroal' /tmp/native-druid.html; then
  echo "datasource monitor: OK"
else
  echo "datasource monitor: FAIL (HTTP $DRUID_CODE)"
  FAILS=$((FAILS+1))
fi

# 定时任务：新增 → 执行一次（Quarkus Scheduler 在 Native 下实际触发）→ 调度日志 → 删除
JOB=$(curl -s -X POST "http://127.0.0.1:2020/monitor/job" -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  --data '{"jobName":"native-smoke-job","jobGroup":"DEFAULT","invokeTarget":"ryTask.ryNoParams","cronExpression":"0 0 0 1 1 ? 2099","misfirePolicy":"3","concurrent":"1","status":"1"}' || true)
JOB_ID=$(curl -s "http://127.0.0.1:2020/monitor/job/list?jobName=native-smoke-job&pageNum=1&pageSize=1" \
  -H "Authorization: Bearer $TOKEN" | grep -o '"jobId":[0-9]*' | head -1 | cut -d: -f2 || true)
if echo "$JOB" | grep -q '"code":200' && [[ -n "$JOB_ID" ]]; then
  curl -s -X PUT "http://127.0.0.1:2020/monitor/job/run" -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' --data "{\"jobId\":$JOB_ID,\"jobGroup\":\"DEFAULT\"}" >/dev/null || true
  sleep 2
  if curl -s "http://127.0.0.1:2020/monitor/jobLog/list?jobName=native-smoke-job&pageNum=1&pageSize=1" \
       -H "Authorization: Bearer $TOKEN" | grep -q '"status":"0"'; then
    echo "job run: OK"
  else
    echo "job run: FAIL (no successful job log)"
    FAILS=$((FAILS+1))
  fi
  curl -s -X DELETE "http://127.0.0.1:2020/monitor/job/$JOB_ID" -H "Authorization: Bearer $TOKEN" >/dev/null || true
else
  echo "job add: FAIL $(echo "$JOB" | head -c 200)"
  FAILS=$((FAILS+1))
fi

# Excel（FastExcel 读写 + XlsxPatcher 图表注入）。check_xlsx name url form-data out [chart]
#   chart=chart：必须含 xl/charts/；chart=data-chart：有数据行（第 3 行起）时才要求含图表
check_xlsx() {
  local name="$1" url="$2" data="$3" out="$4" chart="${5:-}" code
  code=$(curl -s -o "$out" -w '%{http_code}' -X POST "http://127.0.0.1:2020$url" \
    -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/x-www-form-urlencoded' \
    --data "$data" || true)
  if [[ "$code" != "200" ]] || ! head -c 2 "$out" | grep -q 'PK'; then
    echo "$name: FAIL (HTTP $code)"
    FAILS=$((FAILS+1))
    return
  fi
  local parts charts
  parts=$(unzip -l "$out" 2>/dev/null || true)
  charts=$(echo "$parts" | grep -c 'xl/charts/chart' || true)
  local need_chart=false
  if [[ "$chart" == "chart" ]]; then
    need_chart=true
  elif [[ "$chart" == "data-chart" ]] \
       && unzip -p "$out" xl/worksheets/sheet1.xml 2>/dev/null | grep -q '<row r="3"'; then
    need_chart=true
  fi
  if [[ "$need_chart" == "true" && "$charts" -eq 0 ]]; then
    echo "$name: FAIL (missing chart)"
    FAILS=$((FAILS+1))
    return
  fi
  echo "$name: OK ($(wc -c < "$out") bytes, charts=$charts)"
}

check_xlsx "case excel export"      "/system/case/export" "projectId=$PID" /tmp/native-case-export.xlsx
check_xlsx "case import template"   "/system/case/importTemplate?projectId=$PID" "" /tmp/native-case-template.xlsx
check_xlsx "defect excel export"    "/system/defect/export" "projectId=$PID" /tmp/native-defect-export.xlsx
if [[ "$HAS_PROJECT" == "true" ]]; then
  check_xlsx "defect import template" "/system/defect/importTemplate?projectId=$PID" "" /tmp/native-defect-template.xlsx
else
  echo "defect import template: SKIP (fresh DB has no project; 模板需项目模块列表)"
fi
check_xlsx "user excel export"      "/system/user/export" "" /tmp/native-user-export.xlsx
check_xlsx "user import template"   "/system/user/importTemplate" "" /tmp/native-user-template.xlsx
check_xlsx "defect state line"      "/system/defect/statistic/defect-state-line/$PID/export?timeType=day" "" \
  /tmp/native-defect-line.xlsx data-chart
check_xlsx "member defect line"     "/system/defect/statistic/member-defect-line/$PID/export?timeType=month" "" \
  /tmp/native-member-line.xlsx data-chart

if [[ "$HAS_PROJECT" == "true" ]]; then
  NEW_PLAN=$(curl -s -X POST "http://127.0.0.1:2020/system/plan" -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    --data "{\"planName\":\"native-smoke-plan\",\"planVersion\":\"1.0\",\"projectId\":$PID}" || true)
  NEW_PLAN_ID=$(echo "$NEW_PLAN" | grep -o '"planId":"[^"]*"' | head -1 | cut -d'"' -f4 || true)
  check_xlsx "plan statistics" "/system/dashboard/$PID/plan-statistics/export" "" \
    /tmp/native-plan-statistics.xlsx chart
  if [[ -n "$NEW_PLAN_ID" ]]; then
    curl -s -X DELETE "http://127.0.0.1:2020/system/plan/$NEW_PLAN_ID" -H "Authorization: Bearer $TOKEN" >/dev/null || true
  fi
fi

# Excel 导入（FastExcel reader）
if [[ "$HAS_PROJECT" == "true" && -s /tmp/native-case-export.xlsx ]]; then
  IMP=$(curl -s -X POST "http://127.0.0.1:2020/system/case/importData" -H "Authorization: Bearer $TOKEN" \
    -F "file=@/tmp/native-case-export.xlsx" -F "projectId=$PID" || true)
  if echo "$IMP" | grep -q '"code":200'; then
    echo "case excel import: OK"
  else
    echo "case excel import: FAIL $(echo "$IMP" | head -c 200)"
    FAILS=$((FAILS+1))
  fi
fi

# MCP：令牌鉴权 → initialize → tools/list → 通用目录（反射读资源注解）→ 精做工具（回环转发）
if [[ "$HAS_PROJECT" == "true" ]]; then
  MCP_UNAUTH=$(curl -s -o /dev/null -w '%{http_code}' -X POST http://127.0.0.1:2020/mcp -H 'Content-Type: application/json' -d '{}' || true)
  [[ "$MCP_UNAUTH" == "401" ]] && echo "mcp unauthenticated 401: OK" || { echo "mcp unauthenticated: FAIL ($MCP_UNAUTH)"; FAILS=$((FAILS+1)); }
  curl -s -X PUT "http://127.0.0.1:2020/system/project/$PID/mcp" -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' \
    -d '{"enabled":true,"modules":["defect","case","plan","deliverable","report","document","code","dashboard","project"]}' >/dev/null || true
  PAT=$(curl -s -X POST "http://127.0.0.1:2020/system/user/profile/tokens" -H "Authorization: Bearer $TOKEN" \
    -H 'Content-Type: application/json' -d '{"name":"native-smoke","expireDays":1}' \
    | grep -o '"token":"c2b_pat_[^"]*"' | cut -d'"' -f4 || true)
  mcp_post() { # body [session]
    curl -s -D /tmp/native-mcp-headers -X POST http://127.0.0.1:2020/mcp \
      -H "Authorization: Bearer $PAT" -H 'Content-Type: application/json' \
      -H 'Accept: application/json, text/event-stream' ${2:+-H "Mcp-Session-Id: $2"} -d "$1" || true
  }
  if [[ -z "$PAT" ]]; then
    echo "mcp token: FAIL"; FAILS=$((FAILS+1))
  else
    mcp_post '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"native-smoke","version":"1"}}}' >/tmp/native-mcp-init
    SID=$(grep -i '^mcp-session-id:' /tmp/native-mcp-headers | awk '{print $2}' | tr -d '\r' || true)
    mcp_post '{"jsonrpc":"2.0","method":"notifications/initialized"}' "$SID" >/dev/null
    mcp_check() { # name body expected
      local out; out=$(mcp_post "$2" "$SID")
      if echo "$out" | grep -q "$3" && ! echo "$out" | grep -q '"isError":true'; then
        echo "$1: OK"
      else
        echo "$1: FAIL $(echo "$out" | head -c 300)"; FAILS=$((FAILS+1))
      fi
    }
    mcp_check "mcp tools/list" '{"jsonrpc":"2.0","id":2,"method":"tools/list"}' 'search_defects'
    mcp_check "mcp list_operations" '{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"list_operations","arguments":{"project":"'"$PID"'","module":"defect"}}}' 'defect.list'
    mcp_check "mcp search_defects" '{"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"search_defects","arguments":{"project":"'"$PID"'"}}}' 'total'
    mcp_check "mcp get_guide" '{"jsonrpc":"2.0","id":5,"method":"tools/call","params":{"name":"get_guide","arguments":{"topic":"defect"}}}' 'repair_defect'
    mcp_check "mcp prompts/list" '{"jsonrpc":"2.0","id":6,"method":"prompts/list"}' 'fix_defect'
  fi
fi

if [[ "$FAILS" -gt 0 ]]; then
  echo "==> $FAILS checks failed; container log tail:"
  docker logs "$CONTAINER" 2>&1 | grep -iE 'error|exception' | tail -40
  exit 1
fi

echo "Smoke test passed (bin=$(basename "$BIN"))."

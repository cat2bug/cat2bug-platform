#!/usr/bin/env bash
# 公开发布导出：把 SOURCE_REF（默认 HEAD，即 core）的树剔除 deploy/public-release-exclude.txt
# 所列内部路径后，生成一条与私有历史分离的提交，挂到本地分支 public/master。
# 不修改工作区、真实索引与当前分支；不推送，只打印推送命令。
#
# 用法：deploy/scripts/export-public.sh
# 可选环境变量：
#   SOURCE_REF        导出来源（默认 HEAD）
#   PUBLIC_REF        目标 ref（默认 refs/heads/public/master）
#   PUBLIC_BASE_REF   首次导出时的父提交（默认 refs/remotes/github/master，不存在则无父提交）
#   EXCLUDE_FILE      排除清单（默认 deploy/public-release-exclude.txt，读工作区文件）
#   PUBLIC_COMMIT_MESSAGE  提交说明首行
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

SOURCE_REF="${SOURCE_REF:-HEAD}"
PUBLIC_REF="${PUBLIC_REF:-refs/heads/public/master}"
PUBLIC_BASE_REF="${PUBLIC_BASE_REF:-refs/remotes/github/master}"
EXCLUDE_FILE="${EXCLUDE_FILE:-deploy/public-release-exclude.txt}"
PUSH_REMOTES="github public-gitee"
ZERO_OID="0000000000000000000000000000000000000000"

die() { echo "[export-public] 错误：$*" >&2; exit 1; }
log() { echo "[export-public] $*"; }

[ -f "$EXCLUDE_FILE" ] || die "找不到排除清单 $EXCLUDE_FILE"

SOURCE_COMMIT="$(git rev-parse --verify --quiet "${SOURCE_REF}^{commit}")" \
  || die "无法解析 SOURCE_REF=$SOURCE_REF"

EXCLUDES="$(sed -e 's/#.*$//' -e 's/[[:space:]]*$//' -e 's/^[[:space:]]*//' -e 's|^\./||' "$EXCLUDE_FILE" | awk 'NF')"
[ -n "$EXCLUDES" ] || die "排除清单为空：$EXCLUDE_FILE"
while IFS= read -r p; do
  case "$p" in
    /*|*..*) die "排除清单只允许仓库内相对路径：$p" ;;
  esac
done <<EOF
$EXCLUDES
EOF

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/cat2bug-export-public.XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT
TMP_INDEX="$TMP_DIR/index"

# 路径按字面匹配，避免清单中的 * ? [ 被当作 pathspec 通配。
export GIT_LITERAL_PATHSPECS=1

GIT_INDEX_FILE="$TMP_INDEX" git read-tree "${SOURCE_COMMIT}^{tree}"

while IFS= read -r p; do
  GIT_INDEX_FILE="$TMP_INDEX" git ls-files -z -- "$p" \
    | GIT_INDEX_FILE="$TMP_INDEX" git update-index -z --force-remove --stdin
done <<EOF
$EXCLUDES
EOF

NEW_TREE="$(GIT_INDEX_FILE="$TMP_INDEX" git write-tree)"

# 树级自检：目录项按前缀匹配，文件项按全名匹配。
git ls-tree -r -z --name-only "$NEW_TREE" | tr '\0' '\n' > "$TMP_DIR/files"
printf '%s\n' "$EXCLUDES" > "$TMP_DIR/excludes"
LEAKED="$(awk '
  NR == FNR { ex[++n] = $0; next }
  {
    for (i = 1; i <= n; i++) {
      e = ex[i]
      if (substr(e, length(e)) == "/") {
        if (index($0, e) == 1) { print; next }
      } else if ($0 == e || index($0, e "/") == 1) { print; next }
    }
  }' "$TMP_DIR/excludes" "$TMP_DIR/files")"
if [ -n "$LEAKED" ]; then
  printf '%s\n' "$LEAKED" | head -20 >&2 || true
  die "自检失败：导出树仍包含排除路径，未更新 $PUBLIC_REF"
fi
for required in pom.xml cat2bug-platform-ui/ deploy/; do
  awk -v r="$required" 'index($0, r) == 1 { found = 1; exit } END { exit !found }' "$TMP_DIR/files" || die "自检失败：导出树缺少 ${required}，未更新 $PUBLIC_REF"
done
FILE_COUNT="$(wc -l < "$TMP_DIR/files" | tr -d ' ')"

OLD_OID="$(git rev-parse --verify --quiet "${PUBLIC_REF}^{commit}" || true)"
if [ -n "$OLD_OID" ]; then
  PARENT="$OLD_OID"
  if [ "$(git rev-parse "${OLD_OID}^{tree}")" = "$NEW_TREE" ]; then
    log "导出树与 $PUBLIC_REF ($(git rev-parse --short "$OLD_OID")) 相同，无需生成新提交。"
    NEW_COMMIT="$OLD_OID"
  fi
else
  PARENT="$(git rev-parse --verify --quiet "${PUBLIC_BASE_REF}^{commit}" || true)"
  if [ -z "$PARENT" ]; then
    log "首次导出且 $PUBLIC_BASE_REF 不存在，将生成无父提交的根提交。"
  fi
fi

if [ -z "${NEW_COMMIT:-}" ]; then
  MESSAGE="${PUBLIC_COMMIT_MESSAGE:-chore(release): 公开发布导出 $(date '+%Y-%m-%d')}"
  BODY="Source: $(git rev-parse --short=12 "$SOURCE_COMMIT")"
  if [ -n "$PARENT" ]; then
    NEW_COMMIT="$(printf '%s\n\n%s\n' "$MESSAGE" "$BODY" | git commit-tree "$NEW_TREE" -p "$PARENT")"
  else
    NEW_COMMIT="$(printf '%s\n\n%s\n' "$MESSAGE" "$BODY" | git commit-tree "$NEW_TREE")"
  fi
  git update-ref -m "export-public: $SOURCE_REF" "$PUBLIC_REF" "$NEW_COMMIT" "${OLD_OID:-$ZERO_OID}"
  log "已更新 $PUBLIC_REF -> $(git rev-parse --short "$NEW_COMMIT")（父提交：${PARENT:-无}）"
fi

log "来源：$SOURCE_REF ($(git rev-parse --short "$SOURCE_COMMIT"))"
log "公开提交：$NEW_COMMIT"
log "导出文件数：$FILE_COUNT"
if [ -n "$(git --no-optional-locks status --porcelain --untracked-files=no 2>/dev/null)" ]; then
  log "提示：工作区/索引有未提交改动，这些改动不在导出内容中（只导出 $SOURCE_REF 已提交的树）。"
fi

SHORT_REF="${PUBLIC_REF#refs/heads/}"
echo
echo "确认无误后，人工执行推送（本脚本不会推送）："
for remote in $PUSH_REMOTES; do
  if ! git remote get-url "$remote" >/dev/null 2>&1; then
    echo "  # 远端 $remote 未配置，跳过"
    continue
  fi
  remote_head="$(git rev-parse --verify --quiet "refs/remotes/$remote/master^{commit}" || true)"
  if [ -n "$remote_head" ] && ! git merge-base --is-ancestor "$remote_head" "$NEW_COMMIT"; then
    echo "  # 注意：$remote/master 不是 $SHORT_REF 的祖先，下面的推送不是快进，会被拒绝；需人工决定是否强推"
  fi
  echo "  git push $remote $SHORT_REF:master"
done

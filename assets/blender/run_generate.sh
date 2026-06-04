#!/usr/bin/env bash
# Cat2Bug 登录页 3D 场景生成 — 快捷入口
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
PY_SCRIPT="${SCRIPT_DIR}/generate_login_scene.py"

if command -v blender >/dev/null 2>&1; then
  BLENDER="blender"
elif [[ -x "/Applications/Blender.app/Contents/MacOS/Blender" ]]; then
  BLENDER="/Applications/Blender.app/Contents/MacOS/Blender"
else
  echo "错误: 未找到 Blender。请安装 Blender 3.6+ 或将 blender 加入 PATH。" >&2
  exit 1
fi

echo "使用 Blender: ${BLENDER}"
"${BLENDER}" --background --python "${PY_SCRIPT}"
echo "完成。输出目录: ${REPO_ROOT}/cat2bug-platform-ui/src/assets/models/"

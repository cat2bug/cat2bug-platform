#!/usr/bin/env bash
# embedded vs API-only Native 体积对比（arm64）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
ADMIN="$ROOT/cat2bug-platform-admin/target"
CACHE="$ROOT/deploy/.native-size-cache/arm64"
REPORT="$ROOT/readme/spring-native-delivery/VOLUME-ANALYSIS.md"
ARCH=aarch64
EMBEDDED_RAW="$CACHE/cat2bug-admin-linux-arm64.embedded"
EMBEDDED_UPX="$CACHE/cat2bug-admin-linux-arm64.embedded.upx"
API_RAW="$CACHE/cat2bug-admin-linux-arm64.api-only"
API_UPX="$CACHE/cat2bug-admin-linux-arm64.api-only.upx"

bytes_of() { [[ -f "$1" ]] && wc -c < "$1" | tr -d ' ' || echo 0; }
mb_of() { python3 -c "print(round(int('$1')/1024/1024,1))"; }

save_embedded() {
  local raw="$ADMIN/cat2bug-admin-linux-arm64"
  local upx="$ADMIN/cat2bug-admin-linux-arm64.upx"
  [[ -f "$raw" ]] || { echo "[ERROR] 缺少 embedded: $raw" >&2; exit 1; }
  mkdir -p "$CACHE"
  cp -f "$raw" "$EMBEDDED_RAW"
  [[ -f "$upx" ]] && cp -f "$upx" "$EMBEDDED_UPX"
  echo "    embedded raw $(du -h "$EMBEDDED_RAW" | cut -f1) → $CACHE"
}

build_api_only() {
  echo "==> API-only Native 构建（SKIP_EMBEDDED）"
  SKIP_EMBEDDED=true UPX_IN_DOCKER="${UPX_IN_DOCKER:-true}" UPX_SMOKE=false \
    "$ROOT/deploy/build-native-spring.sh" "$ARCH"
  mkdir -p "$CACHE"
  cp -f "$ADMIN/cat2bug-admin-linux-arm64" "$API_RAW"
  [[ -f "$ADMIN/cat2bug-admin-linux-arm64.upx" ]] && \
    cp -f "$ADMIN/cat2bug-admin-linux-arm64.upx" "$API_UPX"
}

jar_analysis() {
  local jar="$ADMIN/cat2bug-admin-exec.jar"
  [[ -f "$jar" ]] || return 0
  python3 <<'PY' "$jar"
import sys, zipfile
from collections import defaultdict
jar = zipfile.ZipFile(sys.argv[1])
d = defaultdict(int)
for i in jar.infolist():
    n = i.filename
    if 'static/' in n: d['static'] += i.file_size
    elif n.startswith('BOOT-INF/lib/'): d['lib'] += i.file_size
    elif n.startswith('BOOT-INF/classes/'): d['classes'] += i.file_size
    if 'kaptcha' in n.lower(): d['kaptcha_entries'] += i.file_size
print('JAR_MB', {k: round(v/1024/1024, 1) for k, v in d.items()})
PY
}

write_report() {
  local emb_raw api_raw emb_b api_b emb_upx_b api_upx_b
  emb_b=$(bytes_of "$EMBEDDED_RAW")
  api_b=$(bytes_of "$API_RAW")
  emb_upx_b=$(bytes_of "$EMBEDDED_UPX")
  api_upx_b=$(bytes_of "$API_UPX")
  local delta_raw delta_upx
  delta_raw=$(python3 -c "print(round(($emb_b-$api_b)/1024/1024,1))")
  delta_upx=$(python3 -c "print(round(($emb_upx_b-$api_upx_b)/1024/1024,1))" 2>/dev/null || echo "n/a")

  mkdir -p "$(dirname "$REPORT")"
  cat > "$REPORT" <<EOF
# Native 体积剖析（自动生成）

**日期:** $(date +%Y-%m-%d)  
**架构:** arm64 embedded vs API-only

## 实测对比

| 产物 | raw (MB) | UPX (MB) |
|------|----------|----------|
| **embedded**（含 SPA） | $(mb_of "$emb_b") | $(mb_of "$emb_upx_b") |
| **API-only**（无 static） | $(mb_of "$api_b") | $(mb_of "$api_upx_b") |
| **差值**（embedded − API-only） | **${delta_raw}** | **${delta_upx}** |

Stretch 目标：raw < 250 MB · UPX < 65 MB

## exec.jar 组成（embedded 构建）

$(jar_analysis 2>/dev/null || echo "（无 exec.jar）")

## 嵌入式 static/ 磁盘分布

\`\`\`
$(du -sh "$ROOT/cat2bug-platform-admin/src/main/resources/static"/* 2>/dev/null | sort -hr | head -8)
\`\`\`

最大 JS chunk：

\`\`\`
$(find "$ROOT/cat2bug-platform-admin/src/main/resources/static/static/js" -name '*.js' -exec du -h {} \; 2>/dev/null | sort -hr | head -8)
\`\`\`

## native-image 仍链接 AWT（高优先级）

aarch64 embedded 构建日志仍产出 \`libawt*.so\` / \`libfontmanager.so\`。**根因：** \`kaptcha-2.3.3.jar\` 仍在 \`cat2bug-framework\` compile 依赖，经 \`unpack-dependencies\` 进入 Native classpath。

**建议：** Native profile 将 kaptcha 设为 \`provided\` 或从 framework 拆到 \`!native\` 模块（Captcha 已走 \`NativeCaptchaSupport\`）。

## 达到 250 / 65 MB 的路径评估

| 措施 | 预估 raw 收益 | 难度 |
|------|---------------|------|
| 去掉 kaptcha → 去除 libawt 链 | **15–40 MB** | 中 |
| API-only + 外置 Nginx 静态 | **≈${delta_raw} MB**（实测差值） | 低（运维分离） |
| Element UI / chunk 瘦身 | **5–15 MB** | 中 |
| Graal \`-Os\`、裁模块 profile | **5–20 MB** | 中–高 |

**结论：** 单 ELF embedded 要稳定 < 250 MB 需 **kaptcha/AWT 剔除 + 前端瘦身**；更易达成的是 **API-only Native + 外置 SPA**（UPX 有望接近 65 MB）。

## 复现

\`\`\`bash
./deploy/scripts/compare-native-sizes.sh
\`\`\`
EOF
  echo "==> 报告: $REPORT"
}

echo "==> compare-native-sizes $(date -Iseconds)"
save_embedded
build_api_only
write_report

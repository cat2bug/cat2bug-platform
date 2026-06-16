## Context

```
readme/production/images/     ~26MB (234 PNG)
        │
        │  CopyWebpackPlugin（build:embedded / build:prod）
        ▼
static/docs/images/           → fat JAR / Native embedded
        │
        │  /system/doc 渲染
        ▼
.markdown-body { max-width: 1000px }
img { max-width: 100% }       （打印样式）
```

**实测基线（2026-06-14，主仓库 `readme/production/images`）：**

| 指标 | 数值 |
|------|------|
| PNG 总数 | 234 |
| 总体积 | ~25.1 MB |
| 宽 > 1600 | 132 张，~13.8 MB |
| 最宽 | 3600px（`case/` 等 UI 截图） |
| MD5 重复组 | 19 组，额外 ~1.3 MB |
| 文档内容区最大宽度 | 1000px |

**反例（关键）：** 对 132 张 `width > 1600` 的 PNG 仅用 ImageMagick `-resize 1600x` + PNG compression-level=9，总体积 **13.76 MB → 23.14 MB（+9.38 MB）**；128/132 文件变大。根因：现有截图 PNG 已高度优化， naive 重编码破坏 palette/过滤压缩。

`backend-prod-packaging-slim` 已将 images 优化标为 Phase 2 Non-Goal；本 design 为其正式方案。

## Goals / Non-Goals

**Goals:**

- 将超宽文档截图降到 **≤1600px 宽**（仅缩小），并经 **pngquant + oxipng** 重压缩
- 消除已知 **重复 PNG**，Markdown 引用 canonical 路径
- 提供可重复执行的 **`optimize:docs-images`** 脚本与 CI 尺寸/体积门禁
- 目检通过后，embedded JAR / Native 构建自动受益（无 Java 代码改动）
- 记录优化前后 metrics（`docs/images` 字节数、超宽文件数）

**Non-Goals:**

- 将 PNG 全面转 WebP（需改 markdown-it 渲染或 `<picture>` 分支，收益/复杂度比低）
- 修改 `/system/doc` UI 布局或 1000px 内容宽
- 压缩 Vue SPA 的 js/css/img（属其他 change）
- 运行时按需加载/外链 CDN 文档图
- 删除 `readme/production` 中非 images 的 Markdown 正文

## Decisions

### 1. 目标宽度：1600px（只缩小）

**决策**：`pixelWidth > 1600` 时，等比缩至 **宽 = 1600**；`pixelWidth ≤ 1600` **不处理尺寸**（禁止放大）。

**理由**：

- 文档区 1000px；1600 源图 ≈ 1.6×，普通 Retina 下可读性足够
- 2000px（2× Retina）更理想但体积收益更小；1600 为探索阶段实测与产品可接受的折中
- 1569px 等边界图若「凑 1600」会放大，实测单文件 +74% 体积

**备选**：1200px 上限——体积更好，2× 屏略软；若目检 1600 收益不足可下调。

### 2. 必须 pngquant，不能裸 resize

**决策**：流水线固定为：

```
输入 PNG
  → (若 w>1600) magick/sips Lanczos 缩至 1600w
  → pngquant --quality=65-90 --skip-if-larger
  → oxipng -o max -strip all
  → 原子替换（写临时文件后 mv）
```

**理由**：探索阶段裸 resize 批量 **+37%** 体积；pngquant palette 量化是 UI 截图类 PNG 的有效手段。`--skip-if-larger` 防止个别文件劣化。

**备选**：仅 oxipng 无损——对已优化 PNG 收益接近 0。

### 3. 源文件就地优化，非构建时临时生成

**决策**：优化结果 **提交进 Git**（`readme/production/images`），`build:embedded` **不**每次跑 pngquant（避免非确定性、拖慢构建）。

**理由**：文档截图变更频率低；CI 校验尺寸即可。构建链保持 `CopyWebpackPlugin` 简单拷贝。

**备选**：webpack loader 构建时压缩——构建慢、难 diff、不利于 PR 审查。

### 4. 去重策略

**决策**：对 MD5 相同 PNG，保留 **`user-guide/**` 深层路径** 为 canonical；删除 `images/case/`、`images/*.png` 等 legacy 重复；更新引用该文件的 Markdown。

**理由**：19 组重复约 1.3MB；零清晰度损失；与现有文档树方向一致。

### 5. CI / 贡献约束

**决策**：

- 脚本 `scripts/check-docs-images.sh`：`width > 1600` 或 `size > 512KB`（可调）→ exit 1
- PR 变更 `readme/production/images/**` 时 CI 运行 check
- `readme/production` 贡献说明：Playwright/系统截图导出建议 viewport **1280–1600**，禁止 3600 全屏导出入库

### 6. 体积验收阈值

**决策**（宽松，防回归）：

| 指标 | 阈值 |
|------|------|
| `readme/production/images` 总字节 | ≤ **22 MB**（优化后目标；当前 ~26 MB） |
| 宽 > 1600 的 PNG 数量 | **0** |
| embedded JAR 内 `static/docs/images` | ≤ **22 MB** |
| 相对 Phase 1 embedded JAR | 额外下降 ≥ **3 MB**（与 images 优化量对齐） |

Native embedded 记录同一 `docs/images` 指标于 `METRICS.md`（spring-native-delivery），不单独设 Native 特化阈值。

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| pngquant 有损，小字/细线糊 | 目检清单：setup 向导、缺陷详情、API doc 代码块旁截图；quality 下限 65；问题文件单独 `--quality=80-95` |
| 裸 resize 反而变大 | 规范写死 pngquant；CI 对比 PR 前后 `du -sb images` |
| 重复路径删错 | 先 `rg` 引用计数；删前保留 list；单测/脚本断言 Markdown 链接无 404 |
| 贡献者提交 3600 图 | CI width check + 文档导出指南 |

## Migration Plan

1. 安装工具依赖文档化：`pngquant`、`oxipng`（或 Docker 一次性镜像）
2. 跑 `optimize:docs-images --dry-run` 出报告 → 团队确认
3. 批量优化 + 去重 + Markdown 路径修正（单 PR 或 images/docs 二分 PR）
4. 目检 `/system/doc` 抽样页
5. 合并后更新 `backend-prod-packaging-slim` / Native METRICS 基线

**回滚**：Git revert 图片 commit；无数据库/配置变更。

## Open Questions

- [ ] 1600 vs 1200：目检后是否下调上限（若 1600 体积收益 < 3MB 则考虑 1200）
- [ ] 是否在 pre-commit hook 强制 check（本地无 pngquant 时是否 skip）
- [ ] `512KB` 单文件上限是否过严（个别复杂截图例外名单）

# prod-docs-images-slim 体积基线

## 优化前（2026-06-14）

| 指标 | 值 |
|------|-----|
| PNG 数量 | 234 |
| 总体积 | ~26 MB |
| 宽 > 1600 | 132 张 |

## 去重后

| 指标 | 值 |
|------|-----|
| 删除重复 PNG | 21 |
| PNG 数量 | 213 |
| 总体积 | ~24 MB |

## 优化后（resize + pngquant + oxipng）

| 指标 | 值 |
|------|-----|
| PNG 数量 | 214 |
| 总体积 | **9.18 MB**（9,630,505 bytes） |
| 宽 > 1600 | **0** |
| 净节省（相对优化前 26MB） | **~17 MB** |
| 净节省（相对去重后 24MB） | **~15 MB** |
| 执行分支/worktree | `feature/spring-native-upx` |

## 流水线统计

- Resized (w>1600): 112
- Smaller after optimize: 202
- Unchanged (~same): 11
- Skipped (would increase): 0

## 验收

- [x] `readme/production/images` ≤ 22 MB（**9.08 MB**）
- [x] `check:docs-images` 通过
- [x] `npm run build:embedded` → `static/docs/images` **9.5 MB**
- [x] embedded JAR（2026-06-14 构建）：**123.7 MB**；`static/docs/images` 解压 **9.08 MB**；无 `stats.json`

相对 Phase 1 基线 ~148MB，JAR 下降约 **~24 MB**（含 images ~17 MB 及仓库内其他瘦身变更）。

## 待手工目检

- [ ] `/system/doc` 抽样页可读性（见 `TESTING.md` §功能）
- [ ] 打印预览无裁切异常

Dry-run 明细见 [METRICS-dry-run.md](./METRICS-dry-run.md)。

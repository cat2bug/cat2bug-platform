## 1. 工具链与脚本

- [x] 1.1 文档化依赖：`pngquant`、`oxipng`（及 macOS/Linux 安装方式）；可选 Docker one-shot 镜像
- [x] 1.2 新增 `scripts/optimize-docs-images.sh`：扫描 `readme/production/images/**/*.png`；`width > 1600` 时 Lanczos 缩至 1600w；**禁止放大**
- [x] 1.3 脚本集成 pngquant（`--quality=65-90 --skip-if-larger`）与 oxipng（`-strip all`）；支持 `--dry-run` 输出前后字节与尺寸报告
- [x] 1.4 新增 `scripts/check-docs-images.sh`：`pixelWidth > 1600` → exit 1；可选单文件 **512KB** 告警（可配置 OFF）
- [x] 1.5 在 `cat2bug-platform-ui/package.json` 增加 `optimize:docs-images`、`check:docs-images` 指向上述脚本

## 2. 批量优化与去重

- [x] 2.1 运行 `optimize:docs-images --dry-run`，保存基线报告（总 MB、超宽数量、预估节省）到 change 目录或 `METRICS.md` 片段
- [x] 2.2 扫描 MD5 重复 PNG（19 组 / ~1.3MB）；生成 canonical 路径表（优先 `user-guide/**`）
- [x] 2.3 更新引用重复图的 Markdown 路径；删除重复 PNG 副本
- [x] 2.4 执行完整 optimize（非 dry-run）；确认 `du -sb readme/production/images` ≤ **22MB**
- [x] 2.5 确认无文件因 skip-if-larger 以外原因体积异常上升（对比 dry-run 报告）

## 3. 目检与可读性

- [ ] 3.1 `/system/doc` 抽样：安装向导（`admin-guide/setup-*`）、缺陷详情、用例 AI、API 文档（含 code-tabs 页）
- [ ] 3.2 打印预览：确认 `img { max-width: 100% }` 下无裁切异常
- [x] 3.3 对 pngquant 后模糊的文件单独提高 quality 重跑并记录例外清单（若有）——无例外，11 张体积基本持平保留原图

## 4. CI 与贡献文档

- [x] 4.1 CI：PR 变更 `readme/production/images/**` 时运行 `check:docs-images`
- [x] 4.2 在 `readme/production` 贡献说明（或 `readme/production/images/README.md`）写截图规范：viewport 1280–1600、禁止 3600 直提、optimize 命令
- [x] 4.3 更新 Playwright/截图脚本（如 `user-register.md` 内嵌脚本）默认 viewport ≤ 1600（若存在）——已为 1280，无需修改

## 5. 打包链与 metrics

- [x] 5.1 确认 `vue.config.js` `CopyWebpackPlugin` 仍整包拷贝 `readme/production` → `static/docs`（无构建时二次压缩）
- [x] 5.2 验收 `npm run build:embedded` + `mvn package -Pembedded`：JAR 无 `stats.json`；`static/docs/images` ≤ **22MB**
- [x] 5.3 记录 embedded JAR 优化前后体积；断言相对 Phase 1 **额外** ≥ **3MB** 下降（或说明未达阈值的阻塞原因）
- [x] 5.4 若 `spring-native-delivery` 分支存在：同步 `METRICS.md` 中 `docs/images` 字节基线（无 Native 特化代码）

## 6. OpenSpec 归档准备

- [x] 6.1 将 `backend-prod-packaging-slim/design.md` Non-Goals 中「压缩 docs/images 留 Phase 2」改为已完成引用（归档时）
- [x] 6.2 编写 `TESTING.md`：check 脚本、optimize dry-run、system/doc 冒烟、JAR 内 images 体积断言命令

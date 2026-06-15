# Native 体积剖析（自动生成）

**日期:** 2026-06-15  
**架构:** arm64 embedded vs API-only

## 实测对比

| 产物 | raw (MB) | UPX (MB) |
|------|----------|----------|
| **embedded**（含 SPA） | 304.3 | 75.6 |
| **API-only**（无 static） | 304.2 | 75.6 |
| **差值**（embedded − API-only） | **0.1** | **0.0** |

> **说明（2026-06-15）：** 当前 `SKIP_EMBEDDED=true` 仅跳过 `npm run build:embedded`，构建仍带 `-Pembedded`，且 `src/main/resources/static/` 已存在上次 embedded 产物，故 **API-only 与 embedded 体积几乎相同**。要测真实 SPA 差值，需在 API-only 构建前临时移走 `static/`（或增加无 embedded profile）后重跑对比。

Stretch 目标：raw < 250 MB · UPX < 65 MB

## exec.jar 组成（embedded 构建）

（无 exec.jar）

## 嵌入式 static/ 磁盘分布

```
 46M	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/static
 11M	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/docs
 44K	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/html
 12K	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/sounds
8.0K	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/index.html
4.0K	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/robots.txt
4.0K	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/index.html.gz
4.0K	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/favicon.ico
```

最大 JS chunk：

```
 12M	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/static/js/app.js
9.3M	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/static/js/chunk-libs.js
7.8M	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/static/js/0.js
2.3M	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/static/js/chunk-elementUI.js
1.7M	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/static/js/1.js
264K	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/static/js/2.js
 68K	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/static/js/3.js
 12K	/Users/yuzhantao/git/cat2bug/cat2bug-platform/cat2bug-platform-admin/src/main/resources/static/static/js/runtime.js
```

## kaptcha 已剔除（2026-06-15）

- `-Pnative`：`kaptcha.scope=provided`；framework/admin **不编译** kaptcha/AWT 源码；`native-maven-plugin` 排除 `kaptcha` 构件
- `exec.jar` / `strings`：**无 kaptcha**；`verify-native-no-kaptcha.sh` 通过
- `ldd`（debian 容器）：**未检出 libawt**（较剔除前：native-image 日志仍生成 `libawt*.so` 中间件，但未链入最终 ELF）
- 体积：raw **304.2 MB**（较 POI 后 embedded **−65 KB**，kaptcha 本身在镜像中占比极小）
- 运行时：`NativeCaptchaSupport` + `CaptchaPngRenderer`；JVM 路径 `JvmCaptchaSupport` / `JvmCaptchaController` 仅在 `!native`

**若需进一步缩体积：** 处理 `FileUploadUtils.uploadBase64Image` 的 `ImageIO`/`BufferedImage`（common 模块），或 Graal 排除 AWT 初始化。

## 达到 250 / 65 MB 的路径评估

| 措施 | 预估 raw 收益 | 难度 |
|------|---------------|------|
| 去掉 kaptcha → 去除 libawt 链 | **≈0 MB**（已剔除；ldd 无 libawt） | 中（**已完成**） |
| API-only + 外置 Nginx 静态 | **≈55 MB**（static 磁盘；本次未真正剔除） | 低（运维分离） |
| Element UI / chunk 瘦身 | **5–15 MB** | 中 |
| Graal `-Os`、裁模块 profile | **5–20 MB** | 中–高 |

**结论：** 单 ELF embedded 要稳定 < 250 MB 需 **kaptcha/AWT 剔除 + 前端瘦身**；外置 SPA 理论可省 **~55 MB**（需构建前移走 `static/` 验证）。

## 复现

```bash
./deploy/scripts/compare-native-sizes.sh
```

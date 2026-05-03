## 开发

```bash
# 克隆项目
git clone https://gitee.com/y_project/RuoYi-Vue

# 进入项目目录
cd ruoyi-ui

# 安装依赖
npm install

# 建议不要直接使用 cnpm 安装依赖，会有各种诡异的 bug。可以通过如下操作解决 npm 下载速度慢的问题
npm install --registry=https://registry.npmmirror.com

# 启动服务
npm run dev
```

浏览器访问 http://localhost:80

## 发布

```bash
# 构建测试环境
npm run build:stage

# 构建生产环境
npm run build:prod
```

## 第三方依赖补丁

本项目使用 [patch-package](https://github.com/ds300/patch-package) 对 `node_modules/` 下的部分依赖打补丁（见 [`patches/`](./patches/) 目录）。

`npm install` 后会通过 `package.json` 中的 `postinstall` 钩子自动应用所有补丁，无需手动操作。

如需新增或修改补丁：

1. 直接修改 `node_modules/<包名>/...` 中的源码；
2. 运行 `npx patch-package <包名>` 生成或更新 `patches/<包名>+<版本>.patch`；
3. 将 `patches/` 下的变更与 `package.json` 一并提交到版本库。

各补丁的用途说明见 [`patches/README.md`](./patches/README.md)。

**提交提醒：** 首次引入补丁时请将 `package.json`、`patches/`、`README.md`（及本段若为新加）一并 `git add` 后提交，避免同事克隆后缺少补丁文件。
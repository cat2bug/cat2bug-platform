# MCP 接入

## 概述

**[@cat2bug/mcp](https://www.npmjs.com/package/@cat2bug/mcp)** 是 Cat2Bug 平台与 AI 助手之间的 **MCP（Model Context Protocol）代理**。安装后由 Cursor、Claude Code 等客户端通过 stdio 启动该进程，即可在对话中查询缺陷、管理用例、推送报告等，底层调用与 [API 介绍](../api/api-intro.md) 相同的 Open API。

| 项目 | 说明 |
|------|------|
| npm 包名 | `@cat2bug/mcp`（当前版本见 [npm 页面](https://www.npmjs.com/package/@cat2bug/mcp)） |
| 全局命令名 | `cat2bug-mcp`（与包名不同，配置 MCP 时使用此命令） |
| 工具数量 | 23 个（缺陷、用例、交付物、成员、报告、文件等） |
| 开源仓库 | [Gitee](https://gitee.com/cat2bug/cat2bug-mcp) |

::: tip
鉴权使用请求头 `CAT2BUG-API-KEY`，**每个项目单独配置 API KEY**。请先完成下方「获取 API KEY」，再配置 MCP。
:::

## 前置条件

- **Node.js 18+**（本机已安装 Node，且 `node`、`npm` 在 PATH 中）
- **Cat2Bug 平台**已可访问（开发、局域网或生产环境）
- 目标项目已创建 **API KEY**（见下一节）

## 获取 API KEY

1. 登录 Cat2Bug，进入要对接 AI 的**项目**；
2. 打开 **项目设置 → API KEY**；
3. 点击 **新建 Key**，妥善保存密钥（勿提交到 Git 或公开渠道）。

配置说明见 [API 授权配置](../api/api-intro.md#api-授权配置)。切换项目时需使用对应项目的 API KEY。

## 安装 @cat2bug/mcp

任选一种方式即可（包发布在 [npm](https://www.npmjs.com/package/@cat2bug/mcp)）。

### 方式 A：npx（推荐，无需全局安装）

每次由 MCP 客户端通过 `npx` 拉取并运行，适合 Cursor 等 IDE 配置：

```bash
npx -y @cat2bug/mcp
```

（亦可用 `pnpm dlx @cat2bug/mcp`、`yarn dlx @cat2bug/mcp`。）

### 方式 B：全局安装

```bash
npm install -g @cat2bug/mcp
# 或：pnpm add -g @cat2bug/mcp
# 或：yarn global add @cat2bug/mcp
```

安装后可直接使用命令 **`cat2bug-mcp`**。

::: tip
全局安装时，`postinstall` 可能将 Claude Code skills 复制到 `~/.claude/skills/`。若不需要（如 CI），安装前可设：`CAT2BUG_MCP_NO_SKILLS=1`。
:::

## 配置平台地址与 API KEY

须让 MCP 进程知道 **Open API 根地址**（`baseUrl`）和 **API KEY**，任选其一即可。

### 填写 baseUrl

与 [API 介绍 · 服务地址](../api/api-intro.md#服务地址baseurl) 一致：

| 场景 | baseUrl 示例 |
|------|----------------|
| 直连后端（MCP 推荐） | `http://<主机>:2020` |
| 经前台 dev 代理 | `http://<主机>:2222/dev-api` |

将 `<主机>` 换为 `localhost` 或局域网 IP（如 `192.168.10.178`）。MCP 一般使用 **直连后端** 地址，不必与浏览器访问前台的 URL 相同。

### 方式 1：MCP 配置中的 env（推荐）

在客户端的 `mcp.json` 里为服务器设置环境变量（见下一节示例）：

| 变量 | 说明 |
|------|------|
| `CAT2BUG_BASE_URL` | Open API 根地址，如 `http://192.168.10.178:2020` |
| `CAT2BUG_API_KEY` | 项目 API KEY |

### 方式 2：配置文件

写入 `~/.cat2bug-mcp/config.json`（权限建议 `600`）：

```json
{
  "baseUrl": "http://192.168.10.178:2020",
  "apiKey": "你的项目 API KEY"
}
```

```bash
chmod 600 ~/.cat2bug-mcp/config.json
```

### 方式 3：对话中 login 工具

MCP 加载后，可在对话中让 AI 调用 **`login`** 工具（参数 `url`、`apiKey`），成功后会写入 `~/.cat2bug-mcp/config.json`。适合首次试通，长期使用仍建议 **env 或配置文件**。

## 在 Cursor 中接入

1. 确认 Node.js 18+ 可用；
2. 打开 **Cursor → Settings → MCP**（或编辑用户/项目级 MCP 配置文件）；
3. 添加服务器，**推荐 npx 方式**（将 `CAT2BUG_BASE_URL`、`CAT2BUG_API_KEY` 改为你的环境）：

```json
{
  "mcpServers": {
    "cat2bug": {
      "command": "npx",
      "args": ["-y", "@cat2bug/mcp"],
      "env": {
        "CAT2BUG_BASE_URL": "http://192.168.10.178:2020",
        "CAT2BUG_API_KEY": "你的项目 API KEY"
      }
    }
  }
}
```

**已全局安装**时，可改为：

```json
{
  "mcpServers": {
    "cat2bug": {
      "command": "cat2bug-mcp",
      "args": [],
      "env": {
        "CAT2BUG_BASE_URL": "http://192.168.10.178:2020",
        "CAT2BUG_API_KEY": "你的项目 API KEY"
      }
    }
  }
}
```

若已配置 `~/.cat2bug-mcp/config.json` 且内容正确，可省略 `env` 中的两项；若工具调用提示未配置，请补全 `env` 或使用 `login`。

4. 保存后 **重启 Cursor** 或重载 MCP；
5. 在对话中确认出现 `listDefects`、`createDefect` 等工具，即可用自然语言操作平台。

## 在 Claude Code 中接入

Claude Code 可使用 `claude mcp add` 或项目 `.mcp.json`，同样指向 `npx` / `cat2bug-mcp` 并配置 `CAT2BUG_BASE_URL`、`CAT2BUG_API_KEY`。详细步骤见上游 [Claude Code 使用指南](https://gitee.com/cat2bug/cat2bug-mcp/blob/master/readme/Claude-Code-%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97.md)（仓库内文档）。

## 日常使用

配置完成并加载工具后，在 AI 对话中直接说明意图即可，例如：

- 「列出当前项目最近 10 条缺陷」
- 「创建缺陷：登录无响应，等级中，指派给 demo」
- 「关闭缺陷编号 1001」

代理会调用对应 MCP 工具；参数含义与平台 [API 文档](../api/api-intro.md) 一致（如处理人使用成员登录账号 `memberAccount`）。

### 可用工具（摘要）

| 分类 | 工具 |
|------|------|
| 认证 | `login` |
| 缺陷 | `listDefects`、`createDefect`、`updateDefect`、`getDefectDetail`、`handleDefect`、`passDefect`、`rejectDefect`、`assignDefect`、`closeDefect`、`openDefect` |
| 项目 / 成员 | `getProjectInfo`、`listMembers` |
| 测试用例 | `listTestCases`、`getTestCaseDetail`、`createTestCase`、`updateTestCase`、`deleteTestCase` |
| 交付物 | `listDeliverables`、`getDeliverableDetail`、`createDeliverable` |
| 报告 / 文件 | `pushDefectReport`、`uploadImage` |

完整参数说明见 npm 包附带的 [工具参考](https://gitee.com/cat2bug/cat2bug-mcp/blob/master/readme/%E5%B7%A5%E5%85%B7%E5%8F%82%E8%80%83.md)。

## 常见问题

### Cursor 里没有 Cat2Bug 工具

- 本机执行 `npx -y @cat2bug/mcp` 是否报错（Node 版本、网络访问 npm）；
- `mcp.json` 中 `command` / `args` 是否与上节示例一致；
- 重启 Cursor，在 MCP 面板查看该服务器是否报错。

### 提示未配置 Base URL / API Key

- 在 `env` 中设置 `CAT2BUG_BASE_URL`、`CAT2BUG_API_KEY`，或维护 `~/.cat2bug-mcp/config.json`；
- 或在对话中先让 AI 执行 `login`。

### API 错误或连不上平台

- 确认 `CAT2BUG_BASE_URL` 从运行 MCP 的机器可访问（端口、防火墙）；
- 确认 API KEY 属于当前要操作的项目；
- 开发环境优先尝试 `http://localhost:2020` 或你的局域网后端地址。

### 升级 MCP 版本

```bash
npm install -g @cat2bug/mcp@latest
```

使用 `npx` 时一般会拉取 registry 上的最新版；若需固定版本，将 args 改为 `["-y", "@cat2bug/mcp@0.1.0"]` 等形式。

## 安全建议

- API KEY 勿写入公开仓库、截图或共享的 `mcp.json` 仓库；
- 生产环境建议单独创建、定期轮换 KEY；
- `~/.cat2bug-mcp/config.json` 保持 `600` 权限。

## 延伸阅读

| 说明 | 链接 |
|------|------|
| npm 包 | [www.npmjs.com/package/@cat2bug/mcp](https://www.npmjs.com/package/@cat2bug/mcp) |
| 平台 API 介绍 | [API 介绍](../api/api-intro.md) |
| 开源仓库 | [Gitee](https://gitee.com/cat2bug/cat2bug-mcp) |
| 快速开始 / 故障排除 | 见仓库 `readme/` 目录 |

从源码构建等进阶内容见上游仓库文档，一般使用 npm 包即可，无需自行克隆编译。

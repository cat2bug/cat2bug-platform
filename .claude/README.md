# Claude Code 团队配置

本目录包含 Cat2Bug Platform 项目的 Claude Code 团队配置，用于规范团队协作流程。

## 目录结构

```
.claude/
├── settings.json           # 主配置文件
├── rules/                  # 规则文件
│   ├── security.md        # 安全规则
│   ├── testing.md         # 测试规则
│   └── coding-style.md    # 代码风格规则
├── agents/                 # Agent 配置
│   ├── planner.md         # 规划专家
│   ├── code-reviewer.md   # 代码审查专家
│   └── build-error-resolver.md  # 构建错误修复专家
└── commands/               # 自定义命令
    ├── plan.md            # /plan 命令
    ├── tdd.md             # /tdd 命令
    ├── build-fix.md       # /build-fix 命令
    └── code-review.md     # /code-review 命令
```

## 核心理念

### 三件事固化团队协作

1. **先把问题说清楚（Plan）**
   - 使用 `/plan` 命令分析需求、识别风险、制定计划
   - 等待确认后再开始编码

2. **再把改动做对（TDD/Build Fix）**
   - 使用 `/tdd` 命令进行测试驱动开发
   - 使用 `/build-fix` 命令增量修复构建错误

3. **最后把风险兜住（Code Review/Security）**
   - 使用 `/code-review` 命令进行代码审查
   - 遵循安全规则，防止漏洞

## 使用方法

### 1. 规划阶段
```
/plan 实现缺陷优先级功能
```
- 复述需求，等待确认
- 识别风险
- 提出方案
- 拆解步骤

### 2. 开发阶段
```
/tdd 实现缺陷创建功能
```
- RED：先写失败的测试
- GREEN：实现最小可用代码
- REFACTOR：重构优化

### 3. 修复阶段
```
/build-fix
```
- 一次只修一个错误
- 修一个验证一个
- 避免连锁反应

### 4. 审查阶段
```
/code-review
```
- 安全审查
- 质量审查
- 测试审查
- 文档审查

## 规则说明

### 安全规则 (security.md)
- 禁止硬编码密钥
- 输入必须校验
- 权限必须控制
- 敏感信息必须脱敏

### 测试规则 (testing.md)
- 新增功能必须有测试
- 修改逻辑必须有测试
- 测试覆盖率 > 70%
- 遵循 TDD 流程

### 代码风格规则 (coding-style.md)
- 命名规范
- 文件组织
- 注释规范
- 错误处理

## Agent 说明

### Planner (规划专家)
- 分析需求
- 识别风险
- 制定计划
- 拆解步骤
- **触发方式**：使用 `/plan` 命令自动切换

### Code Reviewer (代码审查专家)
- 安全审查
- 质量审查
- 测试审查
- 按严重程度分类问题
- **触发方式**：使用 `/code-review` 命令自动切换

### Build Error Resolver (构建错误修复专家)
- 增量修复
- 一次一个
- 立即验证
- 避免连锁
- **触发方式**：使用 `/build-fix` 命令自动切换

## Agent 切换方式

### 自动切换（推荐）
当你使用自定义命令时，会自动切换到对应的 Agent：
```bash
/plan 实现新功能        # 自动使用 planner agent
/code-review           # 自动使用 code-reviewer agent
/build-fix             # 自动使用 build-error-resolver agent
```

### 手动切换
你也可以直接请求使用特定 Agent：
```bash
请使用 planner agent 帮我分析这个需求
请使用 code-reviewer agent 审查我的代码
```

### 默认 Agent
在 `settings.json` 中配置了默认 Agent 为 `planner`，当没有明确指定时使用。

## 权限配置

### 允许的命令
- Maven 构建和测试命令
- npm 构建和开发命令
- Git 查看和提交命令
- 基本文件操作命令

### 禁止的命令
- 危险的删除命令 (rm -rf /)
- 磁盘操作命令 (dd, mkfs)
- 权限修改命令 (chmod -R 777)

### 禁止读取的文件
- 环境变量文件 (.env)
- 生产配置文件 (application-prod.yml)
- 密钥文件 (*.key, *.pem)

## Hooks

### before_file_write
写文件前检查是否包含敏感信息（密码、密钥等）

### before_bash
执行命令前进行安全检查，阻止危险命令

## 团队协作流程

```
需求 → /plan → 确认 → /tdd → 实现 → /build-fix → 修复 → /code-review → 审查 → 合并
```

### 详细流程

1. **需求分析**
   - 使用 `/plan` 命令
   - 复述需求，等待确认
   - 识别风险，提出方案
   - 拆解步骤，明确验收标准

2. **测试驱动开发**
   - 使用 `/tdd` 命令
   - RED：先写失败的测试
   - GREEN：实现最小可用代码
   - REFACTOR：重构优化

3. **构建修复**
   - 使用 `/build-fix` 命令
   - 一次只修一个错误
   - 每次修复后立即验证

4. **代码审查**
   - 使用 `/code-review` 命令
   - 检查安全、质量、测试、文档
   - CRITICAL 问题必须修复

5. **合并代码**
   - 所有测试通过
   - 无 CRITICAL 问题
   - 代码审查通过

## 最佳实践

### DO（推荐）
- ✅ 先规划再编码
- ✅ 先测试再实现
- ✅ 一次修一个错误
- ✅ 提交前代码审查
- ✅ 遵循安全规则
- ✅ 补充测试和文档

### DON'T（避免）
- ❌ 不规划直接编码
- ❌ 不测试直接实现
- ❌ 同时修复多个错误
- ❌ 不审查直接提交
- ❌ 硬编码敏感信息
- ❌ 忽略测试和文档

## 配置更新

### 添加新规则
1. 在 `rules/` 目录创建 Markdown 文件
2. 在 `settings.json` 的 `rules` 数组中添加路径

### 添加新 Agent
1. 在 `agents/` 目录创建 Markdown 文件
2. 在 `settings.json` 的 `agents` 对象中添加配置

### 添加新命令
1. 在 `commands/` 目录创建 Markdown 文件
2. 在 `settings.json` 的 `commands` 对象中添加配置

## 问题反馈

如果配置有问题或需要改进，请：
1. 在团队会议上讨论
2. 提交 Pull Request 修改配置
3. 更新本 README 文档

## 参考资料

- [Claude Code 官方文档](https://claude.ai/docs/claude-code)
- [团队 Starter Kit](https://claudecn.com/docs/claude-code/advanced/starter-kit/)
- [CLAUDE.md 最佳实践](https://claude.ai/docs/claude-code/claude-md)

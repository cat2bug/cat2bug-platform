# Claude Code 快速参考

## 🎯 核心命令

### `/plan` - 规划阶段
**何时使用：** 开始编码前，分析需求和制定计划

**自动切换到：** Planner Agent

**流程：**
```
1. 复述需求（等待确认）
2. 分析现状
3. 识别风险
4. 提出方案
5. 拆解步骤
6. 等待确认后开始实施
```

**示例：**
```bash
/plan 实现缺陷优先级功能
/plan 重构用户认证模块
/plan 修复登录超时问题
```

---

### `/tdd` - 测试驱动开发
**何时使用：** 实现新功能或修改现有逻辑

**流程：**
```
RED    → 先写失败的测试
GREEN  → 实现最小可用代码
REFACTOR → 重构优化
```

**示例：**
```bash
/tdd 实现缺陷创建功能
/tdd 实现用户登录验证
```

---

### `/build-fix` - 修复构建错误
**何时使用：** 构建失败时

**自动切换到：** Build Error Resolver Agent

**原则：**
```
一次只修一个错误
修一个验证一个
避免连锁反应
```

**示例：**
```bash
/build-fix
```

---

### `/code-review` - 代码审查
**何时使用：** 提交代码前

**自动切换到：** Code Reviewer Agent

**审查维度：**
```
🔴 CRITICAL  → 必须修复（阻断合并）
🟠 HIGH      → 强烈建议修复
🟡 MEDIUM    → 建议修复
🟢 LOW       → 可选修复
```

**示例：**
```bash
/code-review
```

---

## 🔄 完整工作流

```
需求 → /plan → 确认 → /tdd → 实现 → /build-fix → 修复 → /code-review → 审查 → 合并
```

### 详细步骤

1. **需求分析**
   ```bash
   /plan 实现缺陷优先级功能
   ```
   - 等待 Planner Agent 分析
   - 确认计划后继续

2. **测试驱动开发**
   ```bash
   /tdd 实现缺陷创建功能
   ```
   - RED: 写测试
   - GREEN: 实现
   - REFACTOR: 重构

3. **修复构建错误**（如果有）
   ```bash
   /build-fix
   ```
   - 一次修一个
   - 立即验证

4. **代码审查**
   ```bash
   /code-review
   ```
   - 检查安全、质量、测试、文档
   - 修复 CRITICAL 问题

5. **提交代码**
   ```bash
   git add .
   git commit -m "feat: 实现缺陷优先级功能"
   ```

---

## 🤖 Agent 切换

### 自动切换（推荐）
使用命令时自动切换到对应 Agent：
```bash
/plan        → Planner Agent
/code-review → Code Reviewer Agent
/build-fix   → Build Error Resolver Agent
```

### 手动切换
直接请求使用特定 Agent：
```bash
请使用 planner agent 帮我分析这个需求
请使用 code-reviewer agent 审查我的代码
请使用 build-error-resolver agent 修复构建错误
```

---

## 📋 规则速查

### 安全规则
- ❌ 禁止硬编码密钥、Token、密码
- ✅ 使用环境变量或配置文件
- ✅ 输入必须校验（@Valid）
- ✅ 权限必须控制（@PreAuthorize）
- ✅ 敏感信息必须脱敏

### 测试规则
- ✅ 新增功能必须有测试
- ✅ 修改逻辑必须有测试
- ✅ 测试覆盖率 > 70%
- ✅ 遵循 TDD 流程（RED → GREEN → REFACTOR）

### 代码风格规则
- Java 类：PascalCase
- Java 方法：camelCase
- 数据库表：snake_case 带前缀
- Vue 组件：PascalCase
- 方法长度：< 50 行
- 类大小：< 500 行

---

## 🚫 常见错误

### ❌ 不要这样做
```bash
# 不规划直接编码
实现缺陷优先级功能  # ❌ 应该先 /plan

# 不测试直接实现
写一个缺陷创建方法  # ❌ 应该用 /tdd

# 同时修复多个错误
修复所有构建错误  # ❌ 应该用 /build-fix 一次修一个

# 不审查直接提交
git commit  # ❌ 应该先 /code-review
```

### ✅ 应该这样做
```bash
# 先规划
/plan 实现缺陷优先级功能

# 测试驱动
/tdd 实现缺陷创建功能

# 增量修复
/build-fix

# 代码审查
/code-review
```

---

## 💡 最佳实践

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

---

## 🔧 常用命令

### 后端
```bash
# 构建
mvn clean package

# 运行
mvn spring-boot:run -pl cat2bug-platform-admin

# 测试
mvn test

# 特定测试
mvn test -Dtest=DefectServiceTest
```

### 前端
```bash
cd cat2bug-platform-ui

# 安装依赖
npm install

# 开发
npm run dev

# 构建
npm run build:prod

# 检查
npm run lint
```

### Git
```bash
# 查看改动
git status
git diff

# 提交
git add .
git commit -m "feat: xxx"
```

---

## 📞 获取帮助

### 查看命令帮助
```bash
/plan --help
/tdd --help
/build-fix --help
/code-review --help
```

### 查看规则
- 安全规则：`.claude/rules/security.md`
- 测试规则：`.claude/rules/testing.md`
- 代码风格：`.claude/rules/coding-style.md`

### 查看 Agent 配置
- Planner：`.claude/agents/planner.md`
- Code Reviewer：`.claude/agents/code-reviewer.md`
- Build Error Resolver：`.claude/agents/build-error-resolver.md`

---

## 🎓 学习资源

- [Claude Code 官方文档](https://claude.ai/docs/claude-code)
- [团队 Starter Kit](https://claudecn.com/docs/claude-code/advanced/starter-kit/)
- [CLAUDE.md 最佳实践](https://claude.ai/docs/claude-code/claude-md)
- 项目配置说明：`.claude/README.md`

# 安全规则

## 密钥与凭证

### 禁止硬编码
- ❌ 禁止在代码中硬编码密钥、Token、密码
- ✅ 使用环境变量或配置文件（不提交到 Git）
- ✅ 敏感配置放在 `application-{profile}.yml` 中，通过 `.gitignore` 排除

### 配置文件安全
- ❌ 禁止提交包含真实凭证的配置文件
- ✅ 提供 `.example` 模板文件
- ✅ 在 `.gitignore` 中排除：
  - `application-prod.yml`
  - `application-local.yml`
  - `.env.local`
  - `*.key`, `*.pem`

## 输入校验

### 边界校验
- ✅ 所有用户输入必须校验（Controller 层使用 `@Valid` + Bean Validation）
- ✅ 文件上传必须校验：
  - 文件类型（白名单）
  - 文件大小（当前限制 500MB）
  - 文件名（防止路径遍历）
- ✅ SQL 参数使用 MyBatis 的 `#{}` 占位符，避免 SQL 注入

### XSS 防护
- ✅ 前端输出时转义 HTML（Element UI 默认转义）
- ✅ 富文本编辑器内容需过滤（使用白名单标签）
- ✅ 后端已配置 XssFilter，但不能完全依赖

## 权限控制

### 最小权限原则
- ✅ Controller 方法必须添加 `@PreAuthorize` 注解
- ✅ 数据权限使用 `@DataScope` 注解
- ✅ 敏感操作（删除、导出）需要额外权限检查

### API 认证
- ✅ 标准接口使用 JWT 认证
- ✅ Open API 使用 API Key 认证
- ❌ 禁止绕过认证（如 `permitAll()` 需有充分理由）

## 日志安全

### 敏感信息脱敏
- ❌ 禁止记录密码、Token、身份证号等敏感信息
- ✅ 记录操作日志时脱敏处理
- ✅ 异常日志避免暴露系统内部信息

## 依赖安全

### 依赖更新
- ✅ 定期检查依赖漏洞（`mvn dependency:tree`）
- ✅ 及时更新有安全漏洞的依赖
- ❌ 避免使用已废弃或无维护的依赖

## 审查清单

在提交代码前，使用 `/code-review` 命令检查：
- [ ] 无硬编码密钥
- [ ] 输入已校验
- [ ] 权限已控制
- [ ] 敏感信息已脱敏
- [ ] 无 SQL 注入风险
- [ ] 无 XSS 风险

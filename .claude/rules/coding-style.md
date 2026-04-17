# 代码风格规则

## 开发环境

### Node.js 版本管理

**当前系统使用 nvm (Node Version Manager) 管理 Node.js 版本**

- 使用 nvm 切换和管理不同的 Node.js 版本
- 项目推荐的 Node.js 版本请参考项目根目录的 `.nvmrc` 文件（如果存在）
- 常用 nvm 命令：
  ```bash
  nvm use          # 使用 .nvmrc 中指定的版本
  nvm use 16       # 切换到 Node.js 16
  nvm current      # 查看当前使用的版本
  nvm ls           # 列出已安装的版本
  ```

### Maven 命令执行

**Maven 命令默认自动确认，无需手动交互**

- 所有 Maven 命令使用 `-B` (batch mode) 参数，自动同意所有提示
- 示例：
  ```bash
  mvn clean install -B          # 批处理模式，自动确认
  mvn test -B                    # 批处理模式运行测试
  mvn spring-boot:run -B         # 批处理模式运行应用
  ```
- 避免使用交互式命令，确保 CI/CD 和自动化流程顺畅

### 文件编辑操作

**文件和命令操作默认自动执行，无需确认**

- 所有文件的读取、编辑、写入、创建、删除操作默认自动执行
- 所有命令的运行、构建、测试操作默认自动执行
- 无需等待用户确认即可直接执行任何操作
- Claude 拥有完整的文件系统和命令执行权限
- 适用于所有代码文件、配置文件、文档文件、脚本文件等
- 注意：涉及敏感文件（如 .env、密钥文件）时仍需谨慎处理

## 文件组织

### 文件大小
- Java 类：建议 < 500 行
- Vue 组件：建议 < 300 行
- 超过建议行数时，考虑拆分

### 方法长度
- 单个方法：建议 < 50 行
- 超过时考虑提取子方法

## 命名规范

### Java
- 类名：PascalCase，名词（`DefectController`, `ProjectService`）
- 方法名：camelCase，动词开头（`getDefectList`, `updateProject`）
- 常量：UPPER_SNAKE_CASE（`MAX_FILE_SIZE`, `DEFAULT_PAGE_SIZE`）
- 包名：全小写，单数形式（`com.cat2bug.system.service`）

### 数据库
- 表名：snake_case，带前缀（`sys_defect`, `sys_project`）
- 字段名：snake_case（`defect_name`, `create_time`）

### Vue
- 组件文件：PascalCase（`DefectList.vue`, `ProjectCard.vue`）
- 组件名：PascalCase（`<DefectList />`）
- Props：camelCase（`defectId`, `projectName`）
- 事件：kebab-case（`@update-status`, `@delete-item`）

## 代码组织

### Java 类结构顺序
```java
public class DefectService {
    // 1. 静态常量
    private static final int MAX_SIZE = 100;

    // 2. 成员变量
    @Autowired
    private DefectMapper defectMapper;

    // 3. 构造函数
    public DefectService() {}

    // 4. 公共方法
    public List<SysDefect> selectDefectList() {}

    // 5. 私有方法
    private void validateDefect() {}
}
```

### Vue 组件结构顺序
```vue
<template>
  <!-- 模板 -->
</template>

<script>
export default {
  name: 'DefectList',
  components: {},
  props: {},
  data() {},
  computed: {},
  watch: {},
  created() {},
  mounted() {},
  methods: {}
}
</script>

<style scoped>
/* 样式 */
</style>
```

## 注释规范

### 何时写注释
- ✅ 复杂业务逻辑
- ✅ 非显而易见的算法
- ✅ 临时解决方案（加 TODO）
- ❌ 不要注释显而易见的代码

### Java 注释
```java
/**
 * 查询缺陷列表
 *
 * @param defect 缺陷查询条件
 * @return 缺陷列表
 */
public List<SysDefect> selectDefectList(SysDefect defect) {
    // 数据权限过滤会通过 DataScopeAspect 自动处理
    return defectMapper.selectDefectList(defect);
}
```

### Vue 注释
```javascript
// 处理缺陷状态变更
handleStatusChange(defect) {
  // 状态变更需要权限验证
  if (!this.hasPermission('system:defect:edit')) {
    this.$message.error('无权限');
    return;
  }
  // ...
}
```

## 错误处理

### Java 异常处理
```java
// ✅ 好的做法
try {
    defectService.updateDefect(defect);
} catch (BusinessException e) {
    log.error("更新缺陷失败: {}", e.getMessage());
    return AjaxResult.error(e.getMessage());
}

// ❌ 避免空 catch
try {
    // ...
} catch (Exception e) {
    // 什么都不做
}

// ❌ 避免吞掉异常
try {
    // ...
} catch (Exception e) {
    return null; // 调用者无法知道发生了什么
}
```

### Vue 错误处理
```javascript
// ✅ 好的做法
try {
  await updateDefect(this.form)
  this.$message.success('更新成功')
  this.getList()
} catch (error) {
  this.$message.error(error.message || '更新失败')
}

// ❌ 避免不处理错误
updateDefect(this.form).then(() => {
  this.$message.success('更新成功')
})
// 如果失败，用户不知道发生了什么
```

## 代码复用

### 提取公共方法
```java
// ❌ 重复代码
public void method1() {
    if (user == null || user.getUserId() == null) {
        throw new BusinessException("用户不存在");
    }
    // ...
}

public void method2() {
    if (user == null || user.getUserId() == null) {
        throw new BusinessException("用户不存在");
    }
    // ...
}

// ✅ 提取公共方法
private void validateUser(SysUser user) {
    if (user == null || user.getUserId() == null) {
        throw new BusinessException("用户不存在");
    }
}
```

### 避免过度抽象
- 三次重复再考虑抽象
- 不要为了"可能的未来需求"过度设计

## 格式化

### Java
- 缩进：4 空格
- 行宽：120 字符
- 大括号：K&R 风格（左括号不换行）

### Vue/JavaScript
- 缩进：2 空格
- 行宽：100 字符
- 分号：使用分号
- 引号：单引号

### 自动格式化
```bash
# Java（如果配置了 Spotless）
mvn spotless:apply

# Vue/JavaScript
cd cat2bug-platform-ui
npm run lint
```

## 导入语句

### Java
- 不使用通配符导入（`import java.util.*`）
- 按字母顺序排列
- 分组：标准库、第三方库、项目内部

### Vue/JavaScript
- 按类型分组：库、组件、工具、样式
- 使用别名：`@/` 表示 `src/`

## 审查清单

- [ ] 命名清晰、符合规范
- [ ] 方法长度合理
- [ ] 有必要的注释
- [ ] 错误处理完善
- [ ] 无重复代码
- [ ] 代码已格式化

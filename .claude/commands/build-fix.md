# /build-fix - 构建错误修复命令

## 用途
增量修复构建错误，避免连锁反应。

## 核心原则
**一次只修一个错误，修一个验证一个**

## 执行流程

### 1. 运行构建
```bash
# 后端
mvn clean compile

# 前端
cd cat2bug-platform-ui && npm run build:prod
```

### 2. 分析错误
从构建输出中提取：
- 错误类型
- 错误位置（文件、行号）
- 错误原因

### 3. 优先级排序
按以下优先级修复：
1. **依赖错误**（缺少依赖、版本冲突）
2. **编译错误**（语法错误、类型错误）
3. **测试失败**（单元测试、集成测试）
4. **打包错误**（资源缺失、配置错误）

### 4. 增量修复
```
选择优先级最高的错误
  ↓
分析错误原因
  ↓
提出修复方案
  ↓
实施修复（只修改必要的代码）
  ↓
运行构建验证
  ↓
成功 → 继续下一个错误
失败 → 回滚并尝试其他方案
```

### 5. 验证修复
每次修复后必须验证：
```bash
# 后端
mvn clean compile

# 如果编译通过，运行测试
mvn test

# 前端
cd cat2bug-platform-ui && npm run build:prod
```

## 常见错误类型

### 1. 依赖错误

#### 缺少依赖
```
错误：cannot find symbol
原因：缺少依赖或导入

修复步骤：
1. 检查 pom.xml 是否有对应依赖
2. 检查 import 语句是否正确
3. 运行 mvn dependency:tree 查看依赖树
```

#### 版本冲突
```
错误：NoSuchMethodError, ClassNotFoundException
原因：依赖版本冲突

修复步骤：
1. 使用 mvn dependency:tree 查看冲突
2. 在 pom.xml 中使用 <exclusions> 排除冲突依赖
3. 或在 <dependencyManagement> 中统一版本
```

### 2. 编译错误

#### 语法错误
```
错误：';' expected, illegal start of expression
原因：语法错误

修复步骤：
1. 检查括号、分号是否匹配
2. 检查关键字拼写
3. 使用 IDE 自动修复
```

#### 类型错误
```
错误：incompatible types, cannot convert
原因：类型不匹配

修复步骤：
1. 检查方法返回类型
2. 检查变量类型
3. 添加类型转换或修改类型定义
```

### 3. 测试失败

#### 断言失败
```
错误：expected:<xxx> but was:<yyy>
原因：测试预期与实际不符

修复步骤：
1. 检查业务逻辑是否正确
2. 检查测试用例是否正确
3. 更新测试或修复代码
```

#### 空指针异常
```
错误：NullPointerException
原因：对象为 null

修复步骤：
1. 添加 null 检查
2. 初始化对象
3. 使用 Optional
```

### 4. 前端构建错误

#### 模块未找到
```
错误：Cannot find module '@/xxx'
原因：模块路径错误或文件不存在

修复步骤：
1. 检查文件路径
2. 检查文件是否存在
3. 检查 import 语句
```

## 约束规则

### 必须遵守
- ✅ 一次只修一个错误
- ✅ 每次修复后立即验证
- ✅ 记录修复过程
- ✅ 如果修复失败，回滚并尝试其他方案
- ❌ 禁止同时修复多个错误
- ❌ 禁止在未验证的情况下继续修复
- ❌ 禁止忽略警告信息
- ❌ 禁止进行不相关的代码改动

### 回滚策略
如果修复引入新错误：
```bash
# 回滚最后一次修改
git checkout -- {文件路径}

# 或使用 git stash
git stash
```

## 输出格式

```markdown
# 构建错误修复

## 初始状态
- 错误数量：{数量}
- 错误类型：{列出类型}

---

## 错误 1：{错误描述}

### 错误信息
```
{完整错误信息}
```

### 分析
- 错误类型：{依赖/编译/测试/打包}
- 错误位置：`{文件路径}:{行号}`
- 错误原因：{原因分析}

### 修复方案
{修复方案说明}

### 实施
{修改的代码或配置}

### 验证
```bash
{验证命令}
```

### 结果
✅ 成功 / ❌ 失败

---

## 错误 2：{错误描述}
...

---

## 最终状态
- 修复成功：{数量}
- 修复失败：{数量}
- 构建状态：✅ 成功 / ❌ 失败

## 总结
{总结修复过程，列出关键问题和解决方案}
```

## 示例

### 示例 1：修复编译错误

```markdown
# 构建错误修复

## 初始状态
- 错误数量：3
- 错误类型：编译错误

---

## 错误 1：cannot find symbol - SysDefect

### 错误信息
```
[ERROR] /path/to/DefectController.java:[45,20] cannot find symbol
  symbol:   class SysDefect
  location: class com.cat2bug.web.controller.DefectController
```

### 分析
- 错误类型：编译错误
- 错误位置：`DefectController.java:45`
- 错误原因：缺少 SysDefect 类的导入

### 修复方案
添加导入语句

### 实施
```java
// 在 DefectController.java 顶部添加
import com.cat2bug.system.domain.SysDefect;
```

### 验证
```bash
mvn clean compile
```

### 结果
✅ 成功，继续修复下一个错误

---

## 错误 2：incompatible types

### 错误信息
```
[ERROR] /path/to/DefectService.java:[78,16] incompatible types: int cannot be converted to Long
```

### 分析
- 错误类型：编译错误
- 错误位置：`DefectService.java:78`
- 错误原因：类型不匹配，方法返回 int 但赋值给 Long

### 修复方案
修改变量类型或添加类型转换

### 实施
```java
// 修改前
Long result = defectMapper.insertDefect(defect);

// 修改后
int result = defectMapper.insertDefect(defect);
```

### 验证
```bash
mvn clean compile
```

### 结果
✅ 成功，继续修复下一个错误

---

## 错误 3：测试失败

### 错误信息
```
[ERROR] DefectServiceTest.testCreateDefect:45 expected:<1> but was:<0>
```

### 分析
- 错误类型：测试失败
- 错误位置：`DefectServiceTest.java:45`
- 错误原因：insertDefect 方法返回值不符合预期

### 修复方案
检查 Service 实现，确保返回正确的值

### 实施
```java
// 检查 DefectServiceImpl.java
@Override
public int insertDefect(SysDefect defect) {
    defect.setCreateTime(DateUtils.getNowDate());
    defect.setCreateBy(SecurityUtils.getUsername());
    // 添加缺失的字段设置
    if (StringUtils.isEmpty(defect.getDefectState())) {
        defect.setDefectState("NEW");
    }
    return defectMapper.insertDefect(defect);
}
```

### 验证
```bash
mvn test -Dtest=DefectServiceTest#testCreateDefect
```

### 结果
✅ 成功

---

## 最终状态
- 修复成功：3
- 修复失败：0
- 构建状态：✅ 成功

## 总结
修复了 3 个错误：
1. 缺少导入语句 - 添加 import
2. 类型不匹配 - 修改变量类型
3. 测试失败 - 补充缺失的字段设置

所有错误已修复，构建成功。
```

## 完成标准

所有错误修复完成后：
```bash
# 后端
mvn clean package  # 必须成功

# 前端
cd cat2bug-platform-ui
npm run build:prod  # 必须成功
```

## 相关命令
- `/plan` - 制定实施计划
- `/tdd` - 使用 TDD 方式开发
- `/code-review` - 代码审查

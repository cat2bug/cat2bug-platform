# 应用类静态 SecureRandom 导致 Native 构建失败

记录新增「静态随机数生成器」的工具类后，JVM 测试全过、Native 构建却失败的问题。

- 日期：2026-10-15
- 涉及：后端 / Native

## 现象

Native 构建（`./deploy/build-native.sh`）在镜像生成阶段报错：

```
Error: Detected an instance of Random/SplittableRandom class in the image heap.
Trace: Object was reached by
  reading static field com.cat2bug.common.security.AccessTokens.RANDOM
```

意思是：构建期创建的 `Random` 实例被放进了镜像堆，种子会被固定，GraalVM 拒绝生成。

## 根因

`AccessTokens` 里有 `private static final SecureRandom RANDOM = new SecureRandom();`。Quarkus 默认在构建期初始化类，`com.cat2bug.common.*` 又被 `NativeRuntimeInitFeature` 整包注册反射而必然可达，于是静态字段在构建期就被赋值并写进镜像。若放行，所有 Native 实例会生成可预测的令牌。

## 解决办法

在 `NativeRuntimeInitFeature#beforeAnalysis` 的 `initializeAtRunTime` 列表中加入 `com.cat2bug.common.security.AccessTokens`，让它在运行期初始化（与 JJWT、JGit `WindowCache` 的处理方式相同）。

## 以后怎么避免

- 新增持有 `Random` / `SecureRandom` / 线程 / SSLContext 的**静态字段**时，同步加到 `NativeRuntimeInitFeature` 的运行期初始化列表，或改为方法内局部创建。
- 涉及新工具类的变更，验收必须包含 Native 构建，JVM 测试覆盖不到这类问题。

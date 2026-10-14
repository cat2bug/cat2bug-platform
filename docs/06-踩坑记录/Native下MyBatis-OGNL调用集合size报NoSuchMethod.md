# Native 下 MyBatis OGNL 调用集合 size() 报 NoSuchMethod

MyBatis XML 里 `<if test="list != null and list.size() > 0">` 在 JVM 正常，Native 镜像里一走到就 500。

- 日期：2026-10-15
- 涉及：后台 / Native

## 现象

- 缺陷列表按状态多选（`params[defectStates]=4`）在 Native 下返回 500，JVM 测试全部通过。
- 日志：`MethodFailedException: Method "size" failed for object [4] [java.lang.NoSuchMethodException: java.util.ArrayList.size()]`。

## 根因

MyBatis 的 OGNL 通过反射查找并调用表达式里的方法。Native 镜像只保留注册过反射的类和方法，`java.util.ArrayList` 等 JDK 集合默认没注册，于是 `size()`、`isEmpty()` 都找不到。只做属性判断（`!= null`）不涉及方法调用，不受影响。

JVM 测试覆盖不到，只有 Native 冒烟或浏览器回归走到对应分支才暴露；`SysCaseMapper`、`SysPlanMapper`、`SysPlanItemMapper` 里早有同样写法。

## 解决办法

- `cat2bug-platform-admin/.../nativeimage/MyBatisOgnlReflectionConfig.java` 用 `@RegisterForReflection` 注册常见集合：`ArrayList`、`LinkedList`、`HashSet`、`LinkedHashSet`、`HashMap`、`LinkedHashMap`，以及按类名注册 `Arrays$ArrayList`、`ImmutableCollections$List12` / `$ListN`（`List.of`、`Stream.toList()`）等内部类。
- 新写的条件优先在 Java 侧把空集合置为 `null`，XML 只判 `!= null`。

## 以后怎么避免

- XML 里出现方法调用（`.size()`、`.isEmpty()`、`.contains()` 等）时，确认传入的集合类型在上面的注册表里；用了别的实现（如 `TreeSet`、`CopyOnWriteArrayList`）要补注册。
- 新增带集合条件的查询，Native 冒烟或浏览器回归要实际走一次该条件。

相关：[[Native下i18n消息源加载失败显示原始key]]、[[Native-Docker登录J2Cache缓存未注册导致NPE]]

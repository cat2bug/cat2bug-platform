# Quarkus 3.18 起空查询参数变为 null

升级到 Quarkus 3.18 及以上后，`?x=` 这种空值查询参数注入到 `String` 时由空串变成 `null`，原本只判空串的代码会空指针。

- 日期：2026-10-15
- 涉及：后台

## 现象

升级到 3.27 前排查发现：`/system/user/profile/updatePwd?oldPassword=&newPassword=` 在 3.15 下 `oldPassword` 是 `""`，校验返回「旧密码错误」；在 3.18 起变为 `null`，`oldPassword.toCharArray()` 抛空指针，接口返回 500。

## 根因

Quarkus 3.18 的 REST 层调整了参数转换：空字符串的 `@QueryParam` / `@FormParam` 注入 `String` 时得到 `null`（迁移指南 3.18 一节）。

## 解决办法

扫描全部 `String` 类型查询参数（154 处），只有 `UserProfileResource.updatePwd` 直接对参数调用方法，补上 `null` 判断：旧密码为 `null` 按「旧密码错误」处理，新密码为 `null` 或空返回「新密码不能为空」。MyBatis 动态 SQL 的条件本来就同时判断 `null` 和空串，不受影响。

## 以后怎么避免

- 资源方法里的 `String` 参数一律按「可能为 `null`」处理，判空用 `StringUtils.isEmpty` 一类同时覆盖两种情况的写法。
- MyBatis 条件保持 `x != null and x != ''` 写法。
- 升级 Quarkus 跨多个小版本时，逐个读迁移指南，把 REST 参数转换类变化列入核对表。

# Quarkus 默认只监听 localhost 导致 App 连不上

- 日期：2026-10-27
- 涉及：后台 / App / 部署

## 现象

改完 Quarkus 后端后，本机浏览器访问 `http://127.0.0.1:2020` 正常，但 `cat2bug-app`（真机、部分模拟器、局域网调试）提示「后端接口连接异常」。`lsof` / `netstat` 显示只监听 `127.0.0.1:2020`，用本机局域网 IP 访问 Connection refused。微信小程序登录路径即使连上也会返回 `code=401`。

## 根因

1. Quarkus 3.27+ 的 `quarkus.http.host` 默认是 localhost（日志 `Listening on: http://localhost:2020`），不再像旧 Spring `server.address=0.0.0.0` 那样默认绑全网卡。Native / RPM 配置里已写 `0.0.0.0`，JVM / `quarkus:dev` 主配置漏了。
2. `/wechat-mp/login`、`/wechat-mp/bind` 在 Spring 里靠 `/**/login`、`/**/bind` 免登录；Quarkus `PublicPaths` 未收录，匿名请求被 `JwtAuthFilter` 打成 401。
3. 旧 Spring `CorsFilter` 未迁到 Quarkus，App H5 跨域预检没有 `Access-Control-Allow-Origin`。

## 解决办法

- 主配置 `application.properties` 显式：`quarkus.http.host=0.0.0.0`，并打开 `quarkus.http.cors=*` 风格配置（origins 用 `/.*/` 以兼容 credentials）。
- `PublicPaths` 将 `wechat-mp/**` 标为公开。
- 真机调试时 App 的「服务器地址」填局域网 IP（`http://192.168.x.x:2020`），不要填手机自己的 `127.0.0.1`。

## 以后怎么避免

- 升级 Quarkus 后核对 `Listening on` 是否仍是全网卡；Native / RPM / JVM 三套配置对 `http.host` 保持一致。
- 从 Spring `SecurityConfig` 迁免登录规则时，把 `/**/login`、`/**/bind` 这类通配拆成明确前缀写进 `PublicPaths`，并在接口清单里标「免登录」。

相关：[[部署]] · [[接口约定]]

# 登录跳转用 router.push({path}) 丢失 query

记录「未登录访问带参数的页面，登录后参数丢了」的问题。

- 日期：2026-10-15
- 涉及：前端

## 现象

未登录打开 OAuth 授权确认页 `/oauth/consent?client_id=…&state=…`，路由守卫跳到 `/login?redirect=%2Foauth%2Fconsent%3Fclient_id%3D…`；登录后回到 `/oauth/consent`，但所有参数都没了，页面报「授权请求无效」。

## 根因

`UserNameAndPasswordLogin.vue` 登录成功后执行 `this.$router.push({ path: redirect })`。`redirect` 是带查询串的完整地址，但以对象的 `path` 字段传入时，vue-router 4 会把 `?` 之后的部分编码进路径或丢弃，不会解析成 query。

## 解决办法

改为 push 字符串：`this.$router.push(redirect)`。同时只接受以 `/` 开头且不以 `//` 开头的站内地址，否则回首页，避免 `redirect=//evil.com` 变成开放跳转。

## 以后怎么避免

- 跳转到「可能带查询串的地址字符串」时，直接 push 字符串，或先解析成 `{ path, query }`；不要把整串塞进 `path`。
- 来自 URL 参数的跳转目标一律校验为站内相对地址。

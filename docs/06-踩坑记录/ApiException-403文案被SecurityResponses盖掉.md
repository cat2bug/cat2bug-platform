# ApiException 403 文案被 SecurityResponses 盖掉

- 日期：2026-11-04
- 涉及：后台 / 前端

## 现象

业务抛出 `ApiException(403, "您已被移出该项目")`，前端收到的却是固定文案「没有权限，请联系管理员授权」，无法按业务文案做引导弹框。

## 根因

`GlobalExceptionMapper` 对 `HTTP_FORBIDDEN` 一律走 `SecurityResponses.forbidden()`，丢掉了 `ApiException` 自带的 `message`。

## 解决办法

403 时若 `ApiException` 带有非空消息，则 `Response.ok(AjaxResult.error(403, msg))` 透传；仅空消息时再用通用 forbidden。

## 以后怎么避免

新增「前端要识别」的 403 业务文案时，确认异常映射不会盖掉 `getMessage()`；联调时看 JSON `msg` 是否与抛出文案一致。

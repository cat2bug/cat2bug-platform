# 后端国际化资源（messages）

与前端 `cat2bug-platform-ui/src/utils/i18n/i18n.js` 语言保持一致：

| 前端 `locale` | 后端资源文件 |
|---------------|----------------|
| `zh_CN`（默认） | `messages.properties` |
| `zh_TW` | `messages_zh_TW.properties` |
| `en_US` | `messages_en_US.properties` |
| `ar` | `messages_ar.properties` |
| `ja_JP` | `messages_ja_JP.properties` |
| `ko_KR` | `messages_ko_KR.properties` |
| `ru` | `messages_ru.properties` |

请求头 `language` 由前端 `setHeader` 传入，由 `MessageLocaleResolver` 解析。

## 维护约定

1. **以 `messages.properties`（简体中文）为基准**：新增或调整键时先改此文件。
2. 在项目根目录执行同步脚本，统一各语言键数量与顺序：

```bash
python3 scripts/sync-i18n-messages.py
```

3. 新增键若需非英文译文，在 `scripts/sync-i18n-messages.py` 的 `LOCALE_OVERRIDES` 中补充审定译文后再次执行同步。

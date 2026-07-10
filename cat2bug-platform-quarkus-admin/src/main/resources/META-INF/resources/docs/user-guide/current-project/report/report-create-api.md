# 通过 API 创建报告

通过 Open API 自动化创建测试报告，实现报告生成的自动化和集成。

## 使用场景

- 自动化生成测试报告
- 集成到 CI/CD 流程
- 定期生成测试报告
- 批量推送缺陷数据

## 推送缺陷报告

### 接口说明

推送缺陷报告接口用于自动化提交测试结果和缺陷数据。

**接口路径：** `/api/report/defect`

**请求方法：** POST

**请求头：**
- `Content-Type: application/json`
- `CAT2BUG-API-KEY: your-api-key`

### 请求示例

```bash
curl --location -X POST 'https://www.cat2bug.com:8022/prod-api/api/report/defect' \
-H 'Content-Type: application/json' \
-H 'CAT2BUG-API-KEY: 20240327001413he1mbyfi6fhnfets' \
-d '{
  "handler": "demo",
  "reportTitle": "单元测试报告:com.cat2bug.junit.demo.Cat2BugRunnerTest",
  "reportTime": 1711546667143,
  "reportDescription": "报告描述",
  "reportData": [
    {
      "defectName": "[testRuleFalse(com.cat2bug.junit.demo.Cat2BugRunnerTest)]",
      "defectDescribe": "缺陷描述",
      "defectState": "PROCESSING",
      "defectType": "BUG",
      "defectLevel": "middle",
      "moduleVersion": "0.0.1",
      "groupKey": "com.cat2bug.junit.demo.Cat2BugRunnerTest",
      "key": "com.cat2bug.junit.demo.Cat2BugRunnerTest.testRuleFalse"
    }
  ]
}'
```

### 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| handler | String | 否 | 处理人，0至30个字节 |
| reportTitle | String | 是 | 报告标题，1至255个字节 |
| reportTime | Long | 是 | 报告时间（时间戳） |
| reportDescription | String | 是 | 报告描述，1至65536个字节 |
| reportData | Array | 否 | 缺陷数组 |

### 缺陷数据参数说明

reportData 数组中每个缺陷对象的属性：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| defectName | String | 是 | 缺陷名称，1至128个字节 |
| defectDescribe | String | 是 | 缺陷描述，1至65536个字节 |
| defectState | String | 否 | 缺陷状态，默认PROCESSING<br>PROCESSING(待处理)、AUDIT(待审核)、REJECTED(已驳回)、CLOSED(已关闭) |
| defectType | String | 否 | 缺陷类型，默认BUG<br>BUG(错误)、TASK(任务)、DEMAND(需求) |
| defectLevel | String | 否 | 缺陷级别，默认MIDDLE<br>LOW(低)、MIDDLE(中)、HEIGHT(高)、URGENT(急) |
| moduleVersion | String | 否 | 交付物的版本号，1至128个字节 |
| groupKey | String | 否 | 缺陷唯一关键组，1至255个字节<br>通过此属性查找相同组的缺陷 |
| key | String | 否 | 缺陷唯一关键字，1至255个字节 |

### 响应示例

```json
{
  "msg": "操作成功",
  "code": 200
}
```

**说明：** code 返回 200 代表推送成功。

## 使用说明

### groupKey 和 key 的作用

**groupKey（缺陷唯一关键组）：**
- 用于标识一组相关的缺陷
- 例如：将某个类的类名作为 groupKey
- 当测试后没有发现此类有问题时，可通过 groupKey 批量更新所有相关缺陷的状态

**key（缺陷唯一关键字）：**
- 用于标识单个缺陷的唯一性
- 避免重复创建相同的缺陷
- 例如：使用"类名.方法名"作为唯一标识

## 相关文档

- [报告接口文档](../../../api/api-report-defect.md) - 完整的 API 接口说明
- [通过模版创建报告](report-create-template.md) - 手动创建报告的方式
- [查看报告](report-view.md) - 查看已创建的报告

::: tip 提示
1. 请妥善保管 API Key，不要泄露给他人
2. 报告时间使用毫秒级时间戳
3. 缺陷描述支持换行符 `\n`
4. 合理使用 groupKey 和 key 避免重复创建缺陷
5. 建议在测试框架中集成此接口，实现自动化报告推送
:::


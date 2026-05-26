# 测试用例接口

项目范围仅由请求头 `CAT2BUG-API-KEY` 绑定，**不要在请求体或查询参数中传 `projectId`**。

Open API 使用**用例编号** `caseNum` 定位用例（详情/删除的路径参数，以及**更新**请求体中的定位字段），使用**交付物全路径** `deliverableName` 关联交付物（多级目录用 `/` 分隔），**不要**传 `caseId`、`moduleId` 等数据库主键类参数。

## 查看测试用例列表

**方法**：GET

**路径**：`/api/case`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| pageNum | 整数 | 否 | 默认1，页码 |
| pageSize | 整数 | 否 | 默认10，页尺寸 |
| caseNum | 长整型 | 否 | 测试用例编号 |
| caseName | 字符型 | 否 | 测试用例名称 |
| deliverableName | 字符型 | 否 | 交付物全路径或节点名称；多级用 `/` 分隔；与 [交付物接口](api-deliverable.md) 中路径规则一致。仅匹配该交付物下的用例，不包含子交付物 |

### Response 返回值

**返回主体说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| total | 长整型 | 是 | 测试用例总数（列表分页总条数） |
| rows | 数组 | 是 | 测试用例对象数组 |
| code | 字符型 | 是 | 返回码，正常返回200 |
| msg | 字符型 | 是 | 接口返回消息 |

**rows 数组中测试用例对象的格式说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| caseNum | 长整型 | 是 | 测试用例编号，每个项目中是唯一值，不同项目中的测试用例编号有可能重复 |
| caseName | 字符型 | 是 | 测试用例名称 |
| caseExpect | 字符型 | 是 | 预期 |
| caseStep | 数组 | 否 | 步骤数组 |
| caseLevel | 整型 | 是 | 用例等级，取值 **1～5**，与产品侧 **P0～P4** 对应：`1`→P0（最高）、`2`→P1、`3`→P2、`4`→P3、`5`→P4（最低）；JSON 中为数字，勿传 `"P0"` 等字符串 |
| casePreconditions | 字符型 | 否 | 前置条件 |
| caseData | 字符型 | 否 | 测试数据 |
| deliverableName | 字符型 | 是 | 关联的交付物**全路径**，多个层级用 `/` 分隔 |
| imgUrlList | 数组 | 否 | 测试用例图片路径集合 |
| remark | 字符型 | 否 | 备注 |

**caseStep 数组中步骤对象的格式说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| stepDescribe | 字符型 | 否 | 步骤描述 |
| stepExpect | 字符型 | 否 | 步骤预期 |

::: code-tabs
```bash title=cURL
curl -G "${baseUrl}/api/case" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  --data-urlencode "pageNum=1" \
  --data-urlencode "pageSize=10" \
  --data-urlencode "caseName=登录" \
  --data-urlencode "deliverableName=登录模块/账号登录"
```
```java title=Java
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

String query = "pageNum=1&pageSize=10"
    + "&caseName=" + URLEncoder.encode("登录", StandardCharsets.UTF_8)
    + "&deliverableName=" + URLEncoder.encode("登录模块/账号登录", StandardCharsets.UTF_8);
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${baseUrl}/api/case?" + query))
    .header("CAT2BUG-API-KEY", "${apiKey}")
    .GET()
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```
```python title=Python
import urllib.parse
import urllib.request

params = urllib.parse.urlencode({
    "pageNum": 1,
    "pageSize": 10,
    "caseName": "登录",
    "deliverableName": "登录模块/账号登录",
})
req = urllib.request.Request(
    f"${baseUrl}/api/case?{params}",
    headers={"CAT2BUG-API-KEY": "${apiKey}"},
    method="GET",
)
with urllib.request.urlopen(req) as resp:
    print(resp.read().decode())
```
```javascript title=Node.js
const params = new URLSearchParams({
  pageNum: "1",
  pageSize: "10",
  caseName: "登录",
  deliverableName: "登录模块/账号登录",
});
const response = await fetch(`${baseUrl}/api/case?${params}`, {
  headers: { "CAT2BUG-API-KEY": "${apiKey}" },
});
console.log(await response.text());
```
```php title=PHP
$query = http_build_query([
    "pageNum" => 1,
    "pageSize" => 10,
    "caseName" => "登录",
    "deliverableName" => "登录模块/账号登录",
]);
$ch = curl_init("${baseUrl}/api/case?" . $query);
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => ["CAT2BUG-API-KEY: ${apiKey}"],
]);
$response = curl_exec($ch);
curl_close($ch);
echo $response;
```
```csharp title=C#
using var client = new HttpClient();
client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
var url = "${baseUrl}/api/case?pageNum=1&pageSize=10"
    + "&caseName=" + Uri.EscapeDataString("登录")
    + "&deliverableName=" + Uri.EscapeDataString("登录模块/账号登录");
var response = await client.GetAsync(url);
Console.WriteLine(await response.Content.ReadAsStringAsync());
```
:::

---

## 查看测试用例详情

**方法**：GET

**路径**：`/api/case/{用例编号}`

路径中的 `{用例编号}` 为当前项目下的 `caseNum`（长整型），与列表返回的 `caseNum` 一致。

### Request 请求参数

无

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| data | 对象 | 是 | 测试用例对象；**字段与列表接口 `rows` 中单条用例对象一致**（同名、同类型、同含义） |
| code | 字符型 | 是 | 返回码，正常返回200 |
| msg | 字符型 | 是 | 接口返回消息 |

**data 对象结构说明**：与上文「**rows 数组中测试用例对象的格式说明**」相同，此处不重复罗列。若用例编号不属于当前 API Key 对应项目或不存在，返回非 200 业务码，`msg` 为「用例不存在或无权限访问」，`data` 为空。

::: code-tabs
```bash title=cURL
curl -X GET "${baseUrl}/api/case/10001" \
  -H "CAT2BUG-API-KEY: ${apiKey}"
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${baseUrl}/api/case/10001"))
    .header("CAT2BUG-API-KEY", "${apiKey}")
    .GET()
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```
```python title=Python
import urllib.request

req = urllib.request.Request(
    "${baseUrl}/api/case/10001",
    headers={"CAT2BUG-API-KEY": "${apiKey}"},
    method="GET",
)
with urllib.request.urlopen(req) as resp:
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch("${baseUrl}/api/case/10001", {
  headers: { "CAT2BUG-API-KEY": "${apiKey}" },
});
console.log(await response.text());
```
```php title=PHP
$ch = curl_init("${baseUrl}/api/case/10001");
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => ["CAT2BUG-API-KEY: ${apiKey}"],
]);
$response = curl_exec($ch);
curl_close($ch);
echo $response;
```
```csharp title=C#
using var client = new HttpClient();
client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
var response = await client.GetAsync("${baseUrl}/api/case/10001");
Console.WriteLine(await response.Content.ReadAsStringAsync());
```
:::

---

## 创建测试用例

**方法**：POST

**路径**：`/api/case`

### Request 请求参数（JSON 体）

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| caseName | 字符型 | 是 | 测试用例名称 |
| caseExpect | 字符型 | 是 | 预期 |
| caseStep | 数组 | 否 | 步骤数组，元素为对象 `[{"stepDescribe":"…","stepExpect":"…"},…]`，不支持将整个 `caseStep` 写成 JSON 字符串 |
| caseLevel | 整型 | 是 | 用例等级，取值 **1～5**，对应 **P0～P4**（含义同列表接口 `caseLevel` 说明） |
| casePreconditions | 字符型 | 否 | 前置条件 |
| caseData | 字符型 | 否 | 测试数据 |
| deliverableName | 字符型 | 否 | 交付物全路径或节点名称，多级用 `/` 分隔；不传表示不关联交付物 |
| remark | 字符型 | 否 | 备注 |

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| data | 对象 | 是 | 创建的测试用例对象，**结构与列表 `rows` 条目一致**（含 `caseNum`、`deliverableName` 等） |
| code | 字符型 | 是 | 返回码，正常返回200 |
| msg | 字符型 | 是 | 接口返回消息 |

::: code-tabs
```bash title=cURL
curl -X POST "${baseUrl}/api/case" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  -H "Content-Type: application/json" \
  -d '{
    "caseName": "用户登录",
    "caseExpect": "登录成功",
    "caseLevel": 2,
    "caseStep": [
      {"stepDescribe": "打开登录页", "stepExpect": "页面展示正常"},
      {"stepDescribe": "输入账号密码", "stepExpect": "校验通过"}
    ],
    "deliverableName": "登录模块/账号登录",
    "remark": "API 创建"
  }'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

String json = """
    {
      "caseName": "用户登录",
      "caseExpect": "登录成功",
      "caseLevel": 2,
      "caseStep": [
        {"stepDescribe": "打开登录页", "stepExpect": "页面展示正常"},
        {"stepDescribe": "输入账号密码", "stepExpect": "校验通过"}
      ],
      "deliverableName": "登录模块/账号登录",
      "remark": "API 创建"
    }
    """;
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${baseUrl}/api/case"))
    .header("CAT2BUG-API-KEY", "${apiKey}")
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(json))
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```
```python title=Python
import json
import urllib.request

body = json.dumps({
    "caseName": "用户登录",
    "caseExpect": "登录成功",
    "caseLevel": 2,
    "caseStep": [
        {"stepDescribe": "打开登录页", "stepExpect": "页面展示正常"},
        {"stepDescribe": "输入账号密码", "stepExpect": "校验通过"},
    ],
    "deliverableName": "登录模块/账号登录",
    "remark": "API 创建",
}).encode()
req = urllib.request.Request(
    "${baseUrl}/api/case",
    data=body,
    headers={
        "CAT2BUG-API-KEY": "${apiKey}",
        "Content-Type": "application/json",
    },
    method="POST",
)
with urllib.request.urlopen(req) as resp:
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch("${baseUrl}/api/case", {
  method: "POST",
  headers: {
    "CAT2BUG-API-KEY": "${apiKey}",
    "Content-Type": "application/json",
  },
  body: JSON.stringify({
    caseName: "用户登录",
    caseExpect: "登录成功",
    caseLevel: 2,
    caseStep: [
      { stepDescribe: "打开登录页", stepExpect: "页面展示正常" },
      { stepDescribe: "输入账号密码", stepExpect: "校验通过" },
    ],
    deliverableName: "登录模块/账号登录",
    remark: "API 创建",
  }),
});
console.log(await response.text());
```
```php title=PHP
$payload = json_encode([
    "caseName" => "用户登录",
    "caseExpect" => "登录成功",
    "caseLevel" => 2,
    "caseStep" => [
        ["stepDescribe" => "打开登录页", "stepExpect" => "页面展示正常"],
        ["stepDescribe" => "输入账号密码", "stepExpect" => "校验通过"],
    ],
    "deliverableName" => "登录模块/账号登录",
    "remark" => "API 创建",
], JSON_UNESCAPED_UNICODE);
$ch = curl_init("${baseUrl}/api/case");
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_POST => true,
    CURLOPT_POSTFIELDS => $payload,
    CURLOPT_HTTPHEADER => [
        "CAT2BUG-API-KEY: ${apiKey}",
        "Content-Type: application/json",
    ],
]);
$response = curl_exec($ch);
curl_close($ch);
echo $response;
```
```csharp title=C#
using System.Net.Http;
using System.Text;
using System.Text.Json;

using var client = new HttpClient();
client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
var json = """
    {
      "caseName": "用户登录",
      "caseExpect": "登录成功",
      "caseLevel": 2,
      "caseStep": [
        {"stepDescribe": "打开登录页", "stepExpect": "页面展示正常"},
        {"stepDescribe": "输入账号密码", "stepExpect": "校验通过"}
      ],
      "deliverableName": "登录模块/账号登录",
      "remark": "API 创建"
    }
    """;
var content = new StringContent(json, Encoding.UTF8, "application/json");
var response = await client.PostAsync("${baseUrl}/api/case", content);
Console.WriteLine(await response.Content.ReadAsStringAsync());
```
:::

---

## 更新测试用例

**方法**：PUT

**路径**：`/api/case`

服务端仅根据请求体中的 **`caseNum`（项目内用例编号）** 查询并更新记录，**不使用**数据库主键 `caseId`；请求体中**不要**传 `caseId`。

### Request 请求参数（JSON 体）

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| caseNum | 长整型 | 是 | 待更新用例的编号（项目内唯一，与列表/详情返回的 `caseNum` 一致）；**勿传 `caseId`** |
| caseName | 字符型 | 否 | 测试用例名称 |
| caseExpect | 字符型 | 否 | 预期 |
| caseStep | 数组 | 否 | 步骤数组，元素为对象 `[{"stepDescribe":"…","stepExpect":"…"},…]`，不支持将整个 `caseStep` 写成 JSON 字符串 |
| caseLevel | 整型 | 否 | 用例等级，取值 **1～5**，对应 **P0～P4**（含义同列表接口 `caseLevel` 说明） |
| casePreconditions | 字符型 | 否 | 前置条件 |
| caseData | 字符型 | 否 | 测试数据 |
| deliverableName | 字符型 | 否 | 交付物全路径或节点名称；传空字符串可解除与交付物的关联；不传表示不修改原有关联 |
| remark | 字符型 | 否 | 备注 |

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| data | 对象 | 是 | 更新后的测试用例对象，**结构与列表 `rows` 条目一致** |
| code | 字符型 | 是 | 返回码，正常返回200 |
| msg | 字符型 | 是 | 接口返回消息 |

::: code-tabs
```bash title=cURL
curl -X PUT "${baseUrl}/api/case" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  -H "Content-Type: application/json" \
  -d '{
    "caseNum": 10001,
    "caseName": "用户登录（修订）",
    "caseExpect": "登录成功并跳转首页",
    "caseLevel": 3,
    "deliverableName": "登录模块/账号登录"
  }'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

String json = """
    {
      "caseNum": 10001,
      "caseName": "用户登录（修订）",
      "caseExpect": "登录成功并跳转首页",
      "caseLevel": 3,
      "deliverableName": "登录模块/账号登录"
    }
    """;
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${baseUrl}/api/case"))
    .header("CAT2BUG-API-KEY", "${apiKey}")
    .header("Content-Type", "application/json")
    .PUT(HttpRequest.BodyPublishers.ofString(json))
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```
```python title=Python
import json
import urllib.request

body = json.dumps({
    "caseNum": 10001,
    "caseName": "用户登录（修订）",
    "caseExpect": "登录成功并跳转首页",
    "caseLevel": 3,
    "deliverableName": "登录模块/账号登录",
}).encode()
req = urllib.request.Request(
    "${baseUrl}/api/case",
    data=body,
    headers={
        "CAT2BUG-API-KEY": "${apiKey}",
        "Content-Type": "application/json",
    },
    method="PUT",
)
with urllib.request.urlopen(req) as resp:
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch("${baseUrl}/api/case", {
  method: "PUT",
  headers: {
    "CAT2BUG-API-KEY": "${apiKey}",
    "Content-Type": "application/json",
  },
  body: JSON.stringify({
    caseNum: 10001,
    caseName: "用户登录（修订）",
    caseExpect: "登录成功并跳转首页",
    caseLevel: 3,
    deliverableName: "登录模块/账号登录",
  }),
});
console.log(await response.text());
```
```php title=PHP
$payload = json_encode([
    "caseNum" => 10001,
    "caseName" => "用户登录（修订）",
    "caseExpect" => "登录成功并跳转首页",
    "caseLevel" => 3,
    "deliverableName" => "登录模块/账号登录",
], JSON_UNESCAPED_UNICODE);
$ch = curl_init("${baseUrl}/api/case");
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_CUSTOMREQUEST => "PUT",
    CURLOPT_POSTFIELDS => $payload,
    CURLOPT_HTTPHEADER => [
        "CAT2BUG-API-KEY: ${apiKey}",
        "Content-Type: application/json",
    ],
]);
$response = curl_exec($ch);
curl_close($ch);
echo $response;
```
```csharp title=C#
using System.Net.Http;
using System.Text;

using var client = new HttpClient();
client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
var json = """
    {
      "caseNum": 10001,
      "caseName": "用户登录（修订）",
      "caseExpect": "登录成功并跳转首页",
      "caseLevel": 3,
      "deliverableName": "登录模块/账号登录"
    }
    """;
var content = new StringContent(json, Encoding.UTF8, "application/json");
var response = await client.PutAsync("${baseUrl}/api/case", content);
Console.WriteLine(await response.Content.ReadAsStringAsync());
```
:::

---

## 删除测试用例

**方法**：DELETE

**路径**：`/api/case/{用例编号}`

路径中的 `{用例编号}` 为当前项目下的 `caseNum`。

### Request 请求参数

无

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| code | 字符型 | 是 | 返回码，正常返回200 |
| msg | 字符型 | 是 | 接口返回消息 |

::: code-tabs
```bash title=cURL
curl -X DELETE "${baseUrl}/api/case/10001" \
  -H "CAT2BUG-API-KEY: ${apiKey}"
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${baseUrl}/api/case/10001"))
    .header("CAT2BUG-API-KEY", "${apiKey}")
    .DELETE()
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```
```python title=Python
import urllib.request

req = urllib.request.Request(
    "${baseUrl}/api/case/10001",
    headers={"CAT2BUG-API-KEY": "${apiKey}"},
    method="DELETE",
)
with urllib.request.urlopen(req) as resp:
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch("${baseUrl}/api/case/10001", {
  method: "DELETE",
  headers: { "CAT2BUG-API-KEY": "${apiKey}" },
});
console.log(await response.text());
```
```php title=PHP
$ch = curl_init("${baseUrl}/api/case/10001");
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_CUSTOMREQUEST => "DELETE",
    CURLOPT_HTTPHEADER => ["CAT2BUG-API-KEY: ${apiKey}"],
]);
$response = curl_exec($ch);
curl_close($ch);
echo $response;
```
```csharp title=C#
using var client = new HttpClient();
client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
var response = await client.DeleteAsync("${baseUrl}/api/case/10001");
Console.WriteLine(await response.Content.ReadAsStringAsync());
```
:::

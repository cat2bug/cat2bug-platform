# 交付物接口

项目范围仅由请求头 `CAT2BUG-API-KEY` 绑定，**不要在请求体或查询参数中传 `projectId`**。

Open API 使用**交付物全路径**（多级用 `/` 分隔）或**节点名称**定位与筛选；响应 JSON 中**不返回** `deliverableId`、`deliverablePid` 等数字主键字段，仅返回名称、全路径、子节点数量、备注等可读字段。

## 查看交付物树形列表

**方法**：GET

**路径**：`/api/deliverable`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| pageNum | 整数 | 否 | 默认1，页码 |
| pageSize | 整数 | 否 | 默认10，页尺寸 |
| parentDeliverablePath | 字符型 | 否 | 父级交付物**全路径**，多级用 `/` 分隔；**不传或传空**表示查询项目根节点下的一级交付物。若路径不存在，返回空列表 |

### Response 返回值

**返回主体说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| total | 长整型 | 是 | 交付物总数 |
| rows | 数组 | 是 | 交付物数组 |
| code | 字符型 | 是 | 返回码，正常返回200 |
| msg | 字符型 | 是 | 接口返回消息 |

**rows 数组中交付物对象的格式说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| deliverableName | 字符型 | 是 | 当前节点名称（单层） |
| deliverablePath | 字符型 | 是 | 自根起的全路径，层级之间用 `/` 分隔 |
| childrenCount | 整型 | 是 | 直接子交付物数量 |
| remark | 字符型 | 否 | 备注 |

::: code-tabs
```bash title=cURL
curl -G "${baseUrl}/api/deliverable" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  --data-urlencode "pageNum=1" \
  --data-urlencode "pageSize=10" \
  --data-urlencode "parentDeliverablePath=模块A"
```
```java title=Java
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

String query = "pageNum=1&pageSize=10"
    + "&parentDeliverablePath=" + URLEncoder.encode("模块A", StandardCharsets.UTF_8);
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${baseUrl}/api/deliverable?" + query))
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
    "parentDeliverablePath": "模块A",
})
req = urllib.request.Request(
    f"${baseUrl}/api/deliverable?{params}",
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
  parentDeliverablePath: "模块A",
});
const response = await fetch(`${baseUrl}/api/deliverable?${params}`, {
  headers: { "CAT2BUG-API-KEY": "${apiKey}" },
});
console.log(await response.text());
```
```php title=PHP
$query = http_build_query([
    "pageNum" => 1,
    "pageSize" => 10,
    "parentDeliverablePath" => "模块A",
]);
$ch = curl_init("${baseUrl}/api/deliverable?" . $query);
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
var url = "${baseUrl}/api/deliverable?pageNum=1&pageSize=10"
    + "&parentDeliverablePath=" + Uri.EscapeDataString("模块A");
var response = await client.GetAsync(url);
Console.WriteLine(await response.Content.ReadAsStringAsync());
```
:::

---

## 查看交付物详情

**方法**：GET

**路径**：`/api/deliverable/info`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| deliverablePath | 字符型 | 是 | 交付物全路径（推荐）或节点名称；与列表返回的 `deliverablePath` / `deliverableName` 规则一致 |

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| data | 对象 | 是 | 交付物对象，字段同列表 `rows` 中单条说明 |
| code | 字符型 | 是 | 返回码，正常返回200 |
| msg | 字符型 | 是 | 接口返回消息 |

若路径或名称无法匹配当前项目下的交付物，返回错误信息，`data` 为空。

::: code-tabs
```bash title=cURL
curl -G "${baseUrl}/api/deliverable/info" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  --data-urlencode "deliverablePath=模块A/子模块B"
```
```java title=Java
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

String query = "deliverablePath=" + URLEncoder.encode("模块A/子模块B", StandardCharsets.UTF_8);
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${baseUrl}/api/deliverable/info?" + query))
    .header("CAT2BUG-API-KEY", "${apiKey}")
    .GET()
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```
```python title=Python
import urllib.parse
import urllib.request

params = urllib.parse.urlencode({"deliverablePath": "模块A/子模块B"})
req = urllib.request.Request(
    f"${baseUrl}/api/deliverable/info?{params}",
    headers={"CAT2BUG-API-KEY": "${apiKey}"},
    method="GET",
)
with urllib.request.urlopen(req) as resp:
    print(resp.read().decode())
```
```javascript title=Node.js
const params = new URLSearchParams({ deliverablePath: "模块A/子模块B" });
const response = await fetch(`${baseUrl}/api/deliverable/info?${params}`, {
  headers: { "CAT2BUG-API-KEY": "${apiKey}" },
});
console.log(await response.text());
```
```php title=PHP
$query = http_build_query(["deliverablePath" => "模块A/子模块B"]);
$ch = curl_init("${baseUrl}/api/deliverable/info?" . $query);
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
var url = "${baseUrl}/api/deliverable/info?deliverablePath="
    + Uri.EscapeDataString("模块A/子模块B");
var response = await client.GetAsync(url);
Console.WriteLine(await response.Content.ReadAsStringAsync());
```
:::

---

## 创建交付物

**方法**：POST

**路径**：`/api/deliverable`

### Request 请求参数（JSON 体）

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|----------|------|
| deliverableName | 字符型 | 是 | 新建节点名称（单层名称，勿含 `/`） |
| parentDeliverablePath | 字符型 | 否 | 父级交付物全路径，多级用 `/` 分隔；不传或传空表示在**项目根**下创建 |
| remark | 字符型 | 否 | 备注 |

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| data | 对象 | 是 | 创建后的交付物对象，字段同列表 `rows` 中单条说明 |
| code | 字符型 | 是 | 返回码，正常返回200 |
| msg | 字符型 | 是 | 接口返回消息 |

::: code-tabs
```bash title=cURL
curl -X POST "${baseUrl}/api/deliverable" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  -H "Content-Type: application/json" \
  -d '{
    "deliverableName": "子模块B",
    "parentDeliverablePath": "模块A",
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
      "deliverableName": "子模块B",
      "parentDeliverablePath": "模块A",
      "remark": "API 创建"
    }
    """;
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${baseUrl}/api/deliverable"))
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
    "deliverableName": "子模块B",
    "parentDeliverablePath": "模块A",
    "remark": "API 创建",
}).encode()
req = urllib.request.Request(
    "${baseUrl}/api/deliverable",
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
const response = await fetch("${baseUrl}/api/deliverable", {
  method: "POST",
  headers: {
    "CAT2BUG-API-KEY": "${apiKey}",
    "Content-Type": "application/json",
  },
  body: JSON.stringify({
    deliverableName: "子模块B",
    parentDeliverablePath: "模块A",
    remark: "API 创建",
  }),
});
console.log(await response.text());
```
```php title=PHP
$payload = json_encode([
    "deliverableName" => "子模块B",
    "parentDeliverablePath" => "模块A",
    "remark" => "API 创建",
], JSON_UNESCAPED_UNICODE);
$ch = curl_init("${baseUrl}/api/deliverable");
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

using var client = new HttpClient();
client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
var json = """
    {
      "deliverableName": "子模块B",
      "parentDeliverablePath": "模块A",
      "remark": "API 创建"
    }
    """;
var content = new StringContent(json, Encoding.UTF8, "application/json");
var response = await client.PostAsync("${baseUrl}/api/deliverable", content);
Console.WriteLine(await response.Content.ReadAsStringAsync());
```
:::

# 缺陷接口文档

处理人相关请求参数均传 **成员登录账号**（字符串，与 [查看项目成员列表](api-member.md#查看项目成员列表) 返回的 `memberAccount` 一致，对应系统用户登录名 `user_name`），**不得传用户数字 ID**。接口 JSON 字段名为 `handlerAccountList`（字符串数组）。

## 查看缺陷列表

**方法**：GET

**路径**：`/api/defect`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|:--------:|------|
| pageNum | 整数 | 否 | 默认 1，页码 |
| pageSize | 整数 | 否 | 默认 10，页尺寸 |
| defectNum | 长整型 | 否 | 缺陷编号 |
| defectName | 字符型 | 否 | 缺陷名称，范围 1–1024 个字符 |
| defectType | 字符型 | 否 | 缺陷类型，范围：`BUG`（错误缺陷）、`TASK`（任务）、`DEMAND`（需求） |
| version | 字符型 | 否 | 版本 |
| defectState | 字符型 | 否 | 缺陷状态，范围：`PROCESSING`（处理中）、`AUDIT`（待验证）、`REJECTED`（已驳回）、`CLOSED`（已关闭） |
| defectLevel | 字符型 | 否 | 缺陷等级，范围：`URGENT`（紧急）、`HEIGHT`（高）、`MIDDLE`（中）、`LOW`（低） |
| handlerAccountList | 数组 | 否 | 处理人登录账号，字符型数组，可多选（与 `memberAccount` 一致，勿传用户 ID） |

### Response 返回值

**返回主体说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| total | 长整型 | 是 | 缺陷总数 |
| rows | 数组 | 是 | 缺陷对象数组 |
| code | 字符型 | 是 | 返回码，正常返回 200 |
| msg | 字符型 | 是 | 接口返回消息 |

**rows 数组中缺陷对象的格式说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| defectNum | 长整型 | 是 | 缺陷编号，每个项目中唯一，不同项目中的缺陷编号可能重复 |
| defectName | 字符型 | 是 | 缺陷名称 |
| defectType | 字符型 | 是 | 缺陷类型，范围：`BUG`、`TASK`、`DEMAND` |
| defectState | 字符型 | 是 | 缺陷状态，范围：`PROCESSING`、`AUDIT`、`REJECTED`、`CLOSED` |
| defectLevel | 字符型 | 是 | 缺陷等级，范围：`URGENT`、`HEIGHT`、`MIDDLE`、`LOW` |
| deliverableName | 字符型 | 否 | 交付物名称，多个层级用 `/` 分隔 |
| version | 字符型 | 否 | 版本 |
| handlerList | 数组 | 否 | 处理人对象集合 |
| imgUrlList | 数组 | 否 | 缺陷图片路径集合 |
| annexUrlList | 数组 | 否 | 缺陷附件路径集合 |
| defectGroupKey | 字符型 | 是 | 缺陷组标识 |
| defectKey | 字符型 | 是 | 缺陷唯一标识 |
| defectDescribe | 字符型 | 否 | 缺陷描述 |
| currentLog | 日志对象 | 是 | 当前最新的缺陷日志 |
| updateTime | 字符型 | 是 | 更新时间，格式 `yyyy-MM-dd HH:mm:ss` |
| creator | 创建人对象 | 是 | 缺陷创建人，结构见下文 [creator 创建人对象](#creator-创建人对象) |

**handlerList 数组中处理人对象的格式说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| memberAccount | 字符型 | 是 | 成员登录账号，唯一值 |
| memberName | 字符型 | 是 | 成员名称（昵称） |
| phoneNumber | 字符型 | 是 | 手机号 |
| email | 字符型 | 否 | 电子邮箱 |

**currentLog 中缺陷日志对象的格式说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| defectLogState | 字符型 | 是 | 缺陷日志状态，范围：`CREATE`（创建）、`ASSIGN`（指派）、`REJECT`（驳回）、`PASS`（通过）、`REJECTED`（已驳回）、`REPAIR`（已处理）、`CLOSED`（已关闭）、`OPEN`（开启）、`UPDATE`（更新） |
| defectLogDescribe | 字符型 | 是 | 缺陷日志描述 |
| createTime | 字符型 | 是 | 日志创建时间，格式 `yyyy-MM-dd HH:mm:ss` |


::: code-tabs
```bash title=cURL
curl -G "${baseUrl}/api/defect?pageNum=1&pageSize=10&handlerAccountList=demo" \
  -H "CAT2BUG-API-KEY: ${apiKey}"
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Example {
  public static void main(String[] args) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl}/api/defect?pageNum=1&pageSize=10&handlerAccountList=demo"))
        .header("CAT2BUG-API-KEY", "${apiKey}")
        .GET()
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.statusCode());
    System.out.println(response.body());
  }
}
```
```python title=Python
import urllib.request

url = "${baseUrl}/api/defect?pageNum=1&pageSize=10&handlerAccountList=demo"
req = urllib.request.Request(url, method="GET", headers={"CAT2BUG-API-KEY": "${apiKey}"})
with urllib.request.urlopen(req) as resp:
    print(resp.status)
    print(resp.read().decode())
```
```javascript title=Node.js
const url = new URL('/api/defect', '${baseUrl}');
url.searchParams.set('pageNum', '1');
url.searchParams.set('pageSize', '10');
url.searchParams.append('handlerAccountList', 'demo');
const response = await fetch(url, { headers: { 'CAT2BUG-API-KEY': '${apiKey}' } });
console.log(response.status, await response.text());
```
```php title=PHP
<?php
$url = '${baseUrl}/api/defect?pageNum=1&pageSize=10&handlerAccountList=demo';
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, ['CAT2BUG-API-KEY: ${apiKey}']);
echo curl_exec($ch);
curl_close($ch);
```
```csharp title=C#
using System;
using System.Net.Http;
using System.Threading.Tasks;

class Program {
  static async Task Main() {
    using var client = new HttpClient();
    client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
    var response = await client.GetAsync("${baseUrl}/api/defect?pageNum=1&pageSize=10&handlerAccountList=demo");
    Console.WriteLine(await response.Content.ReadAsStringAsync());
  }
}
```
:::

---

## 创建缺陷

**方法**：POST

**路径**：`/api/defect`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|:--------:|------|
| defectName | 字符型 | 是 | 缺陷名称，范围 1–128 个字符 |
| defectType | 字符型 | 是 | 缺陷类型，范围：`BUG`、`TASK`、`DEMAND` |
| defectLevel | 字符型 | 是 | 缺陷等级，范围：`URGENT`、`HEIGHT`、`MIDDLE`、`LOW` |
| deliverableName | 字符型 | 否 | 交付物名称，多个层级用 `/` 分隔 |
| version | 字符型 | 否 | 版本 |
| handlerAccountList | 数组 | 是 | 处理人登录账号集合（`memberAccount` 字符串数组，勿传用户 ID） |
| imgUrlList | 数组 | 否 | 缺陷图片路径集合，数组中所有字符串长度和不能超过 5000 |
| annexUrlList | 数组 | 否 | 缺陷附件路径集合，数组中所有字符串长度和不能超过 5000 |
| defectGroupKey | 字符型 | 否 | 缺陷组标识，范围 1–512 个字符；重复提交时变为更新缺陷数据 |
| defectKey | 字符型 | 否 | 缺陷唯一标识，范围 1–1024 个字符；重复提交时变为更新缺陷数据 |
| defectDescribe | 字符型 | 否 | 缺陷描述，范围 0–1024 |

### Response 返回值

**返回主体说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| data | 对象 | 是 | 创建的缺陷对象 |
| code | 字符型 | 是 | 返回码，正常返回 200 |
| msg | 字符型 | 是 | 接口返回消息 |

**data 中缺陷对象的格式说明**（字段含义与列表中缺陷对象一致；含 `handlerList`、`currentLog`、`creator` 等。）


::: code-tabs
```bash title=cURL
curl -X POST "${baseUrl}/api/defect" \
  -H "Content-Type: application/json" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  -d '{"defectName":"示例缺陷","defectType":"BUG","defectLevel":"MIDDLE","handlerAccountList":["demo"]}'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Example {
  public static void main(String[] args) throws Exception {
    String body = "{\"defectName\":\"示例缺陷\",\"defectType\":\"BUG\",\"defectLevel\":\"MIDDLE\",\"handlerAccountList\":[\"demo\"]}";
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl}/api/defect"))
        .header("CAT2BUG-API-KEY", "${apiKey}")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.statusCode());
    System.out.println(response.body());
  }
}
```
```python title=Python
import json
import urllib.request

url = "${baseUrl}/api/defect"
payload = {
    "defectName": "示例缺陷",
    "defectType": "BUG",
    "defectLevel": "MIDDLE",
    "handlerAccountList": [
        "demo"
    ]
}
body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
req = urllib.request.Request(
    url,
    data=body,
    method="POST",
    headers={"CAT2BUG-API-KEY": "${apiKey}", "Content-Type": "application/json"},
)
with urllib.request.urlopen(req) as resp:
    print(resp.status)
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch('${baseUrl}/api/defect', {
  method: 'POST',
  headers: {
    'CAT2BUG-API-KEY': '${apiKey}',
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
  "defectName": "示例缺陷",
  "defectType": "BUG",
  "defectLevel": "MIDDLE",
  "handlerAccountList": [
    "demo"
  ]
}),
});
console.log(response.status, await response.text());
```
```php title=PHP
<?php
$url = '${baseUrl}/api/defect';
$payload = ['defectName' => '示例缺陷', 'defectType' => 'BUG', 'defectLevel' => 'MIDDLE', 'handlerAccountList' => ['demo']];
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'POST');
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload, JSON_UNESCAPED_UNICODE));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['CAT2BUG-API-KEY: ${apiKey}', 'Content-Type: application/json']);
echo curl_exec($ch);
curl_close($ch);
```
```csharp title=C#
using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

class Program {
  static async Task Main() {
    using var client = new HttpClient();
    client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
    var content = new StringContent("{\"defectName\":\"示例缺陷\",\"defectType\":\"BUG\",\"defectLevel\":\"MIDDLE\",\"handlerAccountList\":[\"demo\"]}", Encoding.UTF8, "application/json");
    var response = await client.PostAsync("${baseUrl}/api/defect", content);
    Console.WriteLine(await response.Content.ReadAsStringAsync());
  }
}
```
:::

---

## 修改缺陷

**方法**：PUT

**路径**：`/api/defect`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|:--------:|------|
| defectNum | 长整型 | 是 | 缺陷编号，项目内唯一 |
| defectName | 字符型 | 否 | 缺陷名称，范围 1–128 个字符 |
| defectType | 字符型 | 否 | 缺陷类型，范围：`BUG`、`TASK`、`DEMAND` |
| defectState | 字符型 | 是 | 缺陷状态，范围：`PROCESSING`、`AUDIT`、`REJECTED`、`CLOSED` |
| defectLevel | 字符型 | 否 | 缺陷等级，范围：`URGENT`、`HEIGHT`、`MIDDLE`、`LOW` |
| deliverableName | 字符型 | 否 | 交付物名称，多个层级用 `/` 分隔 |
| version | 字符型 | 否 | 版本 |
| handlerAccountList | 数组 | 否 | 处理人登录账号集合（`memberAccount` 字符串数组，勿传用户 ID） |
| imgUrlList | 数组 | 否 | 缺陷图片路径集合，数组中所有字符串长度和不能超过 5000 |
| annexUrlList | 数组 | 否 | 缺陷附件路径集合，数组中所有字符串长度和不能超过 5000 |
| defectGroupKey | 字符型 | 否 | 缺陷组标识，范围 1–512 个字符 |
| defectKey | 字符型 | 否 | 缺陷唯一标识，范围 1–1024 个字符 |
| defectDescribe | 字符型 | 否 | 缺陷描述，范围 0–1024 |

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| data | 对象 | 是 | 修改后的缺陷对象 |
| code | 字符型 | 是 | 返回码，正常返回 200 |
| msg | 字符型 | 是 | 接口返回消息 |


::: code-tabs
```bash title=cURL
curl -X PUT "${baseUrl}/api/defect" \
  -H "Content-Type: application/json" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  -d '{"defectNum":1001,"defectState":"PROCESSING","handlerAccountList":["demo"]}'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Example {
  public static void main(String[] args) throws Exception {
    String body = "{\"defectNum\":1001,\"defectState\":\"PROCESSING\",\"handlerAccountList\":[\"demo\"]}";
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl}/api/defect"))
        .header("CAT2BUG-API-KEY", "${apiKey}")
        .header("Content-Type", "application/json")
        .PUT(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.statusCode());
    System.out.println(response.body());
  }
}
```
```python title=Python
import json
import urllib.request

url = "${baseUrl}/api/defect"
payload = {
    "defectNum": 1001,
    "defectState": "PROCESSING",
    "handlerAccountList": [
        "demo"
    ]
}
body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
req = urllib.request.Request(
    url,
    data=body,
    method="PUT",
    headers={"CAT2BUG-API-KEY": "${apiKey}", "Content-Type": "application/json"},
)
with urllib.request.urlopen(req) as resp:
    print(resp.status)
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch('${baseUrl}/api/defect', {
  method: 'PUT',
  headers: {
    'CAT2BUG-API-KEY': '${apiKey}',
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
  "defectNum": 1001,
  "defectState": "PROCESSING",
  "handlerAccountList": [
    "demo"
  ]
}),
});
console.log(response.status, await response.text());
```
```php title=PHP
<?php
$url = '${baseUrl}/api/defect';
$payload = ['defectNum' => 1001, 'defectState' => 'PROCESSING', 'handlerAccountList' => ['demo']];
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'PUT');
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload, JSON_UNESCAPED_UNICODE));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['CAT2BUG-API-KEY: ${apiKey}', 'Content-Type: application/json']);
echo curl_exec($ch);
curl_close($ch);
```
```csharp title=C#
using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

class Program {
  static async Task Main() {
    using var client = new HttpClient();
    client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
    var content = new StringContent("{\"defectNum\":1001,\"defectState\":\"PROCESSING\",\"handlerAccountList\":[\"demo\"]}", Encoding.UTF8, "application/json");
    var response = await client.PutAsync("${baseUrl}/api/defect", content);
    Console.WriteLine(await response.Content.ReadAsStringAsync());
  }
}
```
:::

---

## 查看缺陷详情

**方法**：GET

**路径**：`/api/defect/{缺陷编号}`

### Request 请求参数

无

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| data | 对象 | 是 | 缺陷对象 |
| code | 字符型 | 是 | 返回码，正常返回 200 |
| msg | 字符型 | 是 | 接口返回消息 |

**data 中缺陷对象的格式说明**（与列表一致，含 `handlerList`、`currentLog`、`creator` 等。）


::: code-tabs
```bash title=cURL
curl -G "${baseUrl}/api/defect/1001" \
  -H "CAT2BUG-API-KEY: ${apiKey}"
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Example {
  public static void main(String[] args) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl}/api/defect/1001"))
        .header("CAT2BUG-API-KEY", "${apiKey}")
        .GET()
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.statusCode());
    System.out.println(response.body());
  }
}
```
```python title=Python
import urllib.request

url = "${baseUrl}/api/defect/1001"
req = urllib.request.Request(url, method="GET", headers={"CAT2BUG-API-KEY": "${apiKey}"})
with urllib.request.urlopen(req) as resp:
    print(resp.status)
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch('${baseUrl}/api/defect/1001', {
  headers: { 'CAT2BUG-API-KEY': '${apiKey}' },
});
console.log(response.status, await response.text());
```
```php title=PHP
<?php
$url = '${baseUrl}/api/defect/1001';
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, ['CAT2BUG-API-KEY: ${apiKey}']);
echo curl_exec($ch);
curl_close($ch);
```
```csharp title=C#
using System;
using System.Net.Http;
using System.Threading.Tasks;

class Program {
  static async Task Main() {
    using var client = new HttpClient();
    client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
    var response = await client.GetAsync("${baseUrl}/api/defect/1001");
    Console.WriteLine(await response.Content.ReadAsStringAsync());
  }
}
```
:::

---

## creator 创建人对象

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| memberAccount | 字符型 | 是 | 成员登录账号 |
| memberName | 字符型 | 是 | 成员姓名 |
| email | 字符型 | 否 | 邮箱地址 |
| phoneNumber | 字符型 | 否 | 手机号 |
| avatar | 字符型 | 否 | 头像图片网址 |

---

## 缺陷状态与缺陷日志状态对应关系

缺陷与缺陷日志分别记录两套状态：缺陷状态偏业务，日志状态偏历史操作。列表与详情中均会返回，可按需展示。

| 对缺陷的操作 | 缺陷状态 | 日志显示状态 |
|-------------|----------|--------------|
| 创建 | `PROCESSING` 处理中 | `CREATE` 创建 |
| 指派 | `PROCESSING` 处理中（业务上待处理） | `ASSIGN` 指派 |
| 驳回 | `REJECTED` 已驳回 | `REJECTED` 已驳回 |
| 修复 | `AUDIT` 待验证 | `REPAIR` 已处理 |
| 通过 | `CLOSED` 已关闭 | `PASS` 通过 |
| 关闭 | `CLOSED` 已关闭 | `CLOSED` 已关闭 |
| 开启 | `PROCESSING` 处理中 | `OPEN` 开启 |

（具体以接口返回的枚举值为准。）

---

## 修复缺陷

**方法**：POST

**路径**：`/api/defect/{缺陷编号}/repair`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|:--------:|------|
| handlerAccountList | 数组 | 是 | 被指派人登录账号集合，如 `["zhangsan","lisi"]`（`memberAccount`，勿传用户 ID） |
| describe | 字符型 | 否 | 说明，范围 0–255 |

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| data | 对象 | 是 | 缺陷对象，结构同缺陷详情 |
| code | 字符型 | 是 | 返回码，正常返回 200 |
| msg | 字符型 | 是 | 接口返回消息 |


::: code-tabs
```bash title=cURL
curl -X POST "${baseUrl}/api/defect/1001/repair" \
  -H "Content-Type: application/json" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  -d '{"handlerAccountList":["zhangsan","lisi"],"describe":"已修复"}'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Example {
  public static void main(String[] args) throws Exception {
    String body = "{\"handlerAccountList\":[\"zhangsan\",\"lisi\"],\"describe\":\"已修复\"}";
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl}/api/defect/1001/repair"))
        .header("CAT2BUG-API-KEY", "${apiKey}")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.statusCode());
    System.out.println(response.body());
  }
}
```
```python title=Python
import json
import urllib.request

url = "${baseUrl}/api/defect/1001/repair"
payload = {
    "handlerAccountList": [
        "zhangsan",
        "lisi"
    ],
    "describe": "已修复"
}
body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
req = urllib.request.Request(
    url,
    data=body,
    method="POST",
    headers={"CAT2BUG-API-KEY": "${apiKey}", "Content-Type": "application/json"},
)
with urllib.request.urlopen(req) as resp:
    print(resp.status)
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch('${baseUrl}/api/defect/1001/repair', {
  method: 'POST',
  headers: {
    'CAT2BUG-API-KEY': '${apiKey}',
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
  "handlerAccountList": [
    "zhangsan",
    "lisi"
  ],
  "describe": "已修复"
}),
});
console.log(response.status, await response.text());
```
```php title=PHP
<?php
$url = '${baseUrl}/api/defect/1001/repair';
$payload = ['handlerAccountList' => ['zhangsan', 'lisi'], 'describe' => '已修复'];
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'POST');
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload, JSON_UNESCAPED_UNICODE));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['CAT2BUG-API-KEY: ${apiKey}', 'Content-Type: application/json']);
echo curl_exec($ch);
curl_close($ch);
```
```csharp title=C#
using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

class Program {
  static async Task Main() {
    using var client = new HttpClient();
    client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
    var content = new StringContent("{\"handlerAccountList\":[\"zhangsan\",\"lisi\"],\"describe\":\"已修复\"}", Encoding.UTF8, "application/json");
    var response = await client.PostAsync("${baseUrl}/api/defect/1001/repair", content);
    Console.WriteLine(await response.Content.ReadAsStringAsync());
  }
}
```
:::

---

## 通过缺陷

**方法**：POST

**路径**：`/api/defect/{缺陷编号}/pass`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|:--------:|------|
| describe | 字符型 | 否 | 说明，范围 0–255 |

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| data | 对象 | 是 | 缺陷对象，结构同缺陷详情 |
| code | 字符型 | 是 | 返回码，正常返回 200 |
| msg | 字符型 | 是 | 接口返回消息 |


::: code-tabs
```bash title=cURL
curl -X POST "${baseUrl}/api/defect/1001/pass" \
  -H "Content-Type: application/json" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  -d '{"describe":"验证通过"}'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Example {
  public static void main(String[] args) throws Exception {
    String body = "{\"describe\":\"验证通过\"}";
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl}/api/defect/1001/pass"))
        .header("CAT2BUG-API-KEY", "${apiKey}")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.statusCode());
    System.out.println(response.body());
  }
}
```
```python title=Python
import json
import urllib.request

url = "${baseUrl}/api/defect/1001/pass"
payload = {
    "describe": "验证通过"
}
body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
req = urllib.request.Request(
    url,
    data=body,
    method="POST",
    headers={"CAT2BUG-API-KEY": "${apiKey}", "Content-Type": "application/json"},
)
with urllib.request.urlopen(req) as resp:
    print(resp.status)
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch('${baseUrl}/api/defect/1001/pass', {
  method: 'POST',
  headers: {
    'CAT2BUG-API-KEY': '${apiKey}',
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
  "describe": "验证通过"
}),
});
console.log(response.status, await response.text());
```
```php title=PHP
<?php
$url = '${baseUrl}/api/defect/1001/pass';
$payload = ['describe' => '验证通过'];
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'POST');
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload, JSON_UNESCAPED_UNICODE));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['CAT2BUG-API-KEY: ${apiKey}', 'Content-Type: application/json']);
echo curl_exec($ch);
curl_close($ch);
```
```csharp title=C#
using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

class Program {
  static async Task Main() {
    using var client = new HttpClient();
    client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
    var content = new StringContent("{\"describe\":\"验证通过\"}", Encoding.UTF8, "application/json");
    var response = await client.PostAsync("${baseUrl}/api/defect/1001/pass", content);
    Console.WriteLine(await response.Content.ReadAsStringAsync());
  }
}
```
:::

---

## 驳回缺陷

**方法**：POST

**路径**：`/api/defect/{缺陷编号}/reject`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|:--------:|------|
| describe | 字符型 | 否 | 说明，范围 0–255 |

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| data | 对象 | 是 | 缺陷对象，结构同缺陷详情 |
| code | 字符型 | 是 | 返回码，正常返回 200 |
| msg | 字符型 | 是 | 接口返回消息 |


::: code-tabs
```bash title=cURL
curl -X POST "${baseUrl}/api/defect/1001/reject" \
  -H "Content-Type: application/json" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  -d '{"describe":"不符合要求"}'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Example {
  public static void main(String[] args) throws Exception {
    String body = "{\"describe\":\"不符合要求\"}";
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl}/api/defect/1001/reject"))
        .header("CAT2BUG-API-KEY", "${apiKey}")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.statusCode());
    System.out.println(response.body());
  }
}
```
```python title=Python
import json
import urllib.request

url = "${baseUrl}/api/defect/1001/reject"
payload = {
    "describe": "不符合要求"
}
body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
req = urllib.request.Request(
    url,
    data=body,
    method="POST",
    headers={"CAT2BUG-API-KEY": "${apiKey}", "Content-Type": "application/json"},
)
with urllib.request.urlopen(req) as resp:
    print(resp.status)
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch('${baseUrl}/api/defect/1001/reject', {
  method: 'POST',
  headers: {
    'CAT2BUG-API-KEY': '${apiKey}',
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
  "describe": "不符合要求"
}),
});
console.log(response.status, await response.text());
```
```php title=PHP
<?php
$url = '${baseUrl}/api/defect/1001/reject';
$payload = ['describe' => '不符合要求'];
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'POST');
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload, JSON_UNESCAPED_UNICODE));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['CAT2BUG-API-KEY: ${apiKey}', 'Content-Type: application/json']);
echo curl_exec($ch);
curl_close($ch);
```
```csharp title=C#
using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

class Program {
  static async Task Main() {
    using var client = new HttpClient();
    client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
    var content = new StringContent("{\"describe\":\"不符合要求\"}", Encoding.UTF8, "application/json");
    var response = await client.PostAsync("${baseUrl}/api/defect/1001/reject", content);
    Console.WriteLine(await response.Content.ReadAsStringAsync());
  }
}
```
:::

---

## 指派缺陷

**方法**：POST

**路径**：`/api/defect/{缺陷编号}/assign`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|:--------:|------|
| handlerAccountList | 数组 | 是 | 被指派人登录账号集合（`memberAccount`，勿传用户 ID） |
| describe | 字符型 | 否 | 说明，范围 0–255 |

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| data | 对象 | 是 | 缺陷对象，结构同缺陷详情 |
| code | 字符型 | 是 | 返回码，正常返回 200 |
| msg | 字符型 | 是 | 接口返回消息 |


::: code-tabs
```bash title=cURL
curl -X POST "${baseUrl}/api/defect/1001/assign" \
  -H "Content-Type: application/json" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  -d '{"handlerAccountList":["zhangsan"],"describe":"请处理"}'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Example {
  public static void main(String[] args) throws Exception {
    String body = "{\"handlerAccountList\":[\"zhangsan\"],\"describe\":\"请处理\"}";
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl}/api/defect/1001/assign"))
        .header("CAT2BUG-API-KEY", "${apiKey}")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.statusCode());
    System.out.println(response.body());
  }
}
```
```python title=Python
import json
import urllib.request

url = "${baseUrl}/api/defect/1001/assign"
payload = {
    "handlerAccountList": [
        "zhangsan"
    ],
    "describe": "请处理"
}
body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
req = urllib.request.Request(
    url,
    data=body,
    method="POST",
    headers={"CAT2BUG-API-KEY": "${apiKey}", "Content-Type": "application/json"},
)
with urllib.request.urlopen(req) as resp:
    print(resp.status)
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch('${baseUrl}/api/defect/1001/assign', {
  method: 'POST',
  headers: {
    'CAT2BUG-API-KEY': '${apiKey}',
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
  "handlerAccountList": [
    "zhangsan"
  ],
  "describe": "请处理"
}),
});
console.log(response.status, await response.text());
```
```php title=PHP
<?php
$url = '${baseUrl}/api/defect/1001/assign';
$payload = ['handlerAccountList' => ['zhangsan'], 'describe' => '请处理'];
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'POST');
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload, JSON_UNESCAPED_UNICODE));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['CAT2BUG-API-KEY: ${apiKey}', 'Content-Type: application/json']);
echo curl_exec($ch);
curl_close($ch);
```
```csharp title=C#
using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

class Program {
  static async Task Main() {
    using var client = new HttpClient();
    client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
    var content = new StringContent("{\"handlerAccountList\":[\"zhangsan\"],\"describe\":\"请处理\"}", Encoding.UTF8, "application/json");
    var response = await client.PostAsync("${baseUrl}/api/defect/1001/assign", content);
    Console.WriteLine(await response.Content.ReadAsStringAsync());
  }
}
```
:::

---

## 关闭缺陷

**方法**：POST

**路径**：`/api/defect/{缺陷编号}/close`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|:--------:|------|
| describe | 字符型 | 是 | 说明，范围 0–255 |

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| data | 对象 | 是 | 缺陷对象，结构同缺陷详情 |
| code | 字符型 | 是 | 返回码，正常返回 200 |
| msg | 字符型 | 是 | 接口返回消息 |


::: code-tabs
```bash title=cURL
curl -X POST "${baseUrl}/api/defect/1001/close" \
  -H "Content-Type: application/json" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  -d '{"describe":"关闭原因说明"}'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Example {
  public static void main(String[] args) throws Exception {
    String body = "{\"describe\":\"关闭原因说明\"}";
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl}/api/defect/1001/close"))
        .header("CAT2BUG-API-KEY", "${apiKey}")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.statusCode());
    System.out.println(response.body());
  }
}
```
```python title=Python
import json
import urllib.request

url = "${baseUrl}/api/defect/1001/close"
payload = {
    "describe": "关闭原因说明"
}
body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
req = urllib.request.Request(
    url,
    data=body,
    method="POST",
    headers={"CAT2BUG-API-KEY": "${apiKey}", "Content-Type": "application/json"},
)
with urllib.request.urlopen(req) as resp:
    print(resp.status)
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch('${baseUrl}/api/defect/1001/close', {
  method: 'POST',
  headers: {
    'CAT2BUG-API-KEY': '${apiKey}',
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
  "describe": "关闭原因说明"
}),
});
console.log(response.status, await response.text());
```
```php title=PHP
<?php
$url = '${baseUrl}/api/defect/1001/close';
$payload = ['describe' => '关闭原因说明'];
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'POST');
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload, JSON_UNESCAPED_UNICODE));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['CAT2BUG-API-KEY: ${apiKey}', 'Content-Type: application/json']);
echo curl_exec($ch);
curl_close($ch);
```
```csharp title=C#
using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

class Program {
  static async Task Main() {
    using var client = new HttpClient();
    client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
    var content = new StringContent("{\"describe\":\"关闭原因说明\"}", Encoding.UTF8, "application/json");
    var response = await client.PostAsync("${baseUrl}/api/defect/1001/close", content);
    Console.WriteLine(await response.Content.ReadAsStringAsync());
  }
}
```
:::

---

## 开启缺陷

**方法**：POST

**路径**：`/api/defect/{缺陷编号}/open`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|:--------:|------|
| describe | 字符型 | 是 | 说明，范围 0–255 |

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| data | 对象 | 是 | 缺陷对象，结构同缺陷详情 |
| code | 字符型 | 是 | 返回码，正常返回 200 |
| msg | 字符型 | 是 | 接口返回消息 |
::: code-tabs
```bash title=cURL
curl -X POST "${baseUrl}/api/defect/1001/open" \
  -H "Content-Type: application/json" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  -d '{"describe":"重新开启"}'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Example {
  public static void main(String[] args) throws Exception {
    String body = "{\"describe\":\"重新开启\"}";
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("${baseUrl}/api/defect/1001/open"))
        .header("CAT2BUG-API-KEY", "${apiKey}")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    System.out.println(response.statusCode());
    System.out.println(response.body());
  }
}
```
```python title=Python
import json
import urllib.request

url = "${baseUrl}/api/defect/1001/open"
payload = {
    "describe": "重新开启"
}
body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
req = urllib.request.Request(
    url,
    data=body,
    method="POST",
    headers={"CAT2BUG-API-KEY": "${apiKey}", "Content-Type": "application/json"},
)
with urllib.request.urlopen(req) as resp:
    print(resp.status)
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch('${baseUrl}/api/defect/1001/open', {
  method: 'POST',
  headers: {
    'CAT2BUG-API-KEY': '${apiKey}',
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
  "describe": "重新开启"
}),
});
console.log(response.status, await response.text());
```
```php title=PHP
<?php
$url = '${baseUrl}/api/defect/1001/open';
$payload = ['describe' => '重新开启'];
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_CUSTOMREQUEST, 'POST');
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload, JSON_UNESCAPED_UNICODE));
curl_setopt($ch, CURLOPT_HTTPHEADER, ['CAT2BUG-API-KEY: ${apiKey}', 'Content-Type: application/json']);
echo curl_exec($ch);
curl_close($ch);
```
```csharp title=C#
using System;
using System.Net.Http;
using System.Text;
using System.Threading.Tasks;

class Program {
  static async Task Main() {
    using var client = new HttpClient();
    client.DefaultRequestHeaders.Add("CAT2BUG-API-KEY", "${apiKey}");
    var content = new StringContent("{\"describe\":\"重新开启\"}", Encoding.UTF8, "application/json");
    var response = await client.PostAsync("${baseUrl}/api/defect/1001/open", content);
    Console.WriteLine(await response.Content.ReadAsStringAsync());
  }
}
```
:::


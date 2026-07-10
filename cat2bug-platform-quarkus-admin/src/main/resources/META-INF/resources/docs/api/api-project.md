# 项目接口

## 获取项目信息

**方法**：GET

**路径**：`/api/project`

### Request 请求参数

无（从请求头的 CAT2BUG-API-KEY 中获取项目信息）

### Response 返回值

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| data | 对象 | 是 | 项目信息对象 |
| code | 字符型 | 是 | 返回码，正常返回200 |
| msg | 字符型 | 是 | 接口返回消息 |

**data 对象结构说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| projectName | 字符型 | 是 | 项目名称 |
| projectIntroduce | 字符型 | 否 | 项目介绍 |
| createTime | 日期型 | 是 | 创建时间 |
| creator | 对象 | 是 | 创建成员信息 |
| statistics | 对象 | 是 | 统计信息 |

**creator 对象结构说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| memberAccount | 字符型 | 是 | 成员账号 |
| memberName | 字符型 | 是 | 成员名称 |
| phoneNumber | 字符型 | 是 | 手机号 |
| email | 字符型 | 否 | 电子邮箱 |

**statistics 对象结构说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|----------|------|
| defectCount | 整型 | 是 | 缺陷数量 |
| caseCount | 整型 | 是 | 用例数量 |
| memberCount | 整型 | 是 | 成员数量 |
| deliverableCount | 整型 | 是 | 交付物数量 |

::: code-tabs
```bash title=cURL
curl --location -X GET '${baseUrl}/api/project' \
  -H 'CAT2BUG-API-KEY: ${apiKey}'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${baseUrl}/api/project"))
    .header("CAT2BUG-API-KEY", "${apiKey}")
    .GET()
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```
```python title=Python
import urllib.request

url = "${baseUrl}/api/project"
req = urllib.request.Request(
    url,
    method="GET",
    headers={"CAT2BUG-API-KEY": "${apiKey}"},
)
with urllib.request.urlopen(req) as resp:
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch(`${baseUrl}/api/project`, {
  method: 'GET',
  headers: {
    'CAT2BUG-API-KEY': '${apiKey}',
  },
});
console.log(await response.text());
```
```php title=PHP
$ch = curl_init('${baseUrl}/api/project');
curl_setopt_array($ch, [
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => [
        'CAT2BUG-API-KEY: ${apiKey}',
    ],
]);
$response = curl_exec($ch);
curl_close($ch);
echo $response;
```
```csharp title=C#
using System.Net.Http;

var client = new HttpClient();
var request = new HttpRequestMessage(HttpMethod.Get, "${baseUrl}/api/project");
request.Headers.Add("CAT2BUG-API-KEY", "${apiKey}");
var response = await client.SendAsync(request);
Console.WriteLine(await response.Content.ReadAsStringAsync());
```
:::

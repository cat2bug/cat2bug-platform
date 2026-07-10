# 成员接口

## 查看项目成员列表

**方法**：GET

**路径**：`/api/member`

### Request 请求参数

无（项目由请求头 `CAT2BUG-API-KEY` 确定）

### Response 返回值

**返回主体说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| total | 长整型 | 是 | 成员总数 |
| rows | 数组 | 是 | 用户数组 |
| code | 字符型 | 是 | 返回码，正常返回 200 |
| msg | 字符型 | 是 | 接口返回消息 |

**rows 数组中成员对象的格式说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| memberAccount | 字符型 | 是 | 成员登录账号，唯一值；缺陷接口中 `handlerAccountList` 传此字段，**勿传用户数字 ID** |
| memberName | 字符型 | 是 | 成员名称 |
| phoneNumber | 字符型 | 是 | 手机号 |
| email | 字符型 | 否 | 电子邮箱 |
| roleNameList | 数组 | 是 | 角色名称数组，范围包括：团队创建人、团队管理员、团队普通人员、项目创建人、项目管理员、项目经理、开发、测试、外部人员等（以实际返回为准） |

::: code-tabs
```bash title=cURL
curl --location -X GET '${baseUrl}/api/member' \
  -H 'CAT2BUG-API-KEY: ${apiKey}'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${baseUrl}/api/member"))
    .header("CAT2BUG-API-KEY", "${apiKey}")
    .GET()
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```
```python title=Python
import urllib.request

url = "${baseUrl}/api/member"
req = urllib.request.Request(
    url,
    method="GET",
    headers={"CAT2BUG-API-KEY": "${apiKey}"},
)
with urllib.request.urlopen(req) as resp:
    print(resp.read().decode())
```
```javascript title=Node.js
const response = await fetch(`${baseUrl}/api/member`, {
  method: 'GET',
  headers: {
    'CAT2BUG-API-KEY': '${apiKey}',
  },
});
console.log(await response.text());
```
```php title=PHP
$ch = curl_init('${baseUrl}/api/member');
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
var request = new HttpRequestMessage(HttpMethod.Get, "${baseUrl}/api/member");
request.Headers.Add("CAT2BUG-API-KEY", "${apiKey}");
var response = await client.SendAsync(request);
Console.WriteLine(await response.Content.ReadAsStringAsync());
```
:::

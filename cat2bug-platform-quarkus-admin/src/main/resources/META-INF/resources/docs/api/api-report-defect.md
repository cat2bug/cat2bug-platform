# 报告接口

## 推送缺陷报告

* 接口路径: `/api/report/defect`

* 方法: POST

* 请求参数

```json
{
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
            "moduleVersion":"0.0.1",
            "groupKey": "com.cat2bug.junit.demo.Cat2BugRunnerTest",
            "key": "com.cat2bug.junit.demo.Cat2BugRunnerTest.testRuleFalse"
        }
    ]
}
```

请求参数说明：

| 参数 | 类型 | 范围 | 说明 |
|------|------|------|------|
| handler | 字符型 | 非必填，0至30个字节 | 处理人登录账号（与成员 `memberAccount` 一致，如 `demo`），**勿传用户数字 ID** |
| reportTitle | 字符型 | 必填，1至255个字节 | 报告标题 |
| reportTime | 长整型 | 必填 | 报告时间 |
| reportDescription | 字符型 | 必填，1至65536个字节 | 报告描述 |
| reportData | JSON数组 | 非必填 | 缺陷数组 |

reportData 数组中的格式属性说明：

| 参数 | 类型 | 范围 | 说明 |
|------|------|------|------|
| defectName | 字符型 | 必填，1至128个字节 | 缺陷名称 |
| defectDescribe | 字符型 | 必填，1至65536个字节 | 缺陷描述 |
| defectState | 字符型 | 非必填，默认PROCESSING；PROCESSING(待处理)、AUDIT(待审核)、REJECTED(已驳回)、CLOSED(已关闭) | 缺陷状态 |
| defectType | 字符型 | 非必填，默认BUG；BUG(错误)、TASK(任务)、DEMAND(需求) | 缺陷类型 |
| defectLevel | 字符型 | 非必填，默认MIDDLE；LOW(低)、MIDDLE(中)、HEIGHT(高)、URGENT(急) | 缺陷级别 |
| moduleVersion | 字符型 | 非必填，1至128个字节 | 交付物的版本号 |
| groupKey | 字符型 | 非必填，1至255个字节 | 缺陷唯一关键组，通过此属性查找相同组的缺陷。如将某个类的类名作为groupKey，当测试后没有发现此类有问题，可通过这个groupKey属性来改变所有此类关联的缺陷状态。 |
| key | 字符型 | 非必填，1至255个字节 | 缺陷唯一关键字 |

* 返回值，code 返回 200 代表推送成功。

```json
{
    "msg":"操作成功",
    "code":200
}
```

::: code-tabs
```bash title=cURL
curl --location -X POST '${baseUrl}/api/report/defect' \
  -H 'Content-Type: application/json' \
  -H 'CAT2BUG-API-KEY: ${apiKey}' \
  -d '{"handler":"demo","reportTitle":"单元测试报告:com.cat2bug.junit.demo.Cat2BugRunnerTest","reportTime":1711546667143,"reportDescription":"报告描述","reportData":[{"defectName":"[testRuleFalse(com.cat2bug.junit.demo.Cat2BugRunnerTest)]","defectDescribe":"缺陷描述","defectState":"PROCESSING","defectType":"BUG","defectLevel":"middle","moduleVersion":"0.0.1","groupKey":"com.cat2bug.junit.demo.Cat2BugRunnerTest","key":"com.cat2bug.junit.demo.Cat2BugRunnerTest.testRuleFalse"}]}'
```
```java title=Java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

HttpClient client = HttpClient.newHttpClient();
String body = "{\"handler\":\"demo\",\"reportTitle\":\"单元测试报告:com.cat2bug.junit.demo.Cat2BugRunnerTest\",\"reportTime\":1711546667143,\"reportDescription\":\"报告描述\",\"reportData\":[{\"defectName\":\"[testRuleFalse(com.cat2bug.junit.demo.Cat2BugRunnerTest)]\",\"defectDescribe\":\"缺陷描述\",\"defectState\":\"PROCESSING\",\"defectType\":\"BUG\",\"defectLevel\":\"middle\",\"moduleVersion\":\"0.0.1\",\"groupKey\":\"com.cat2bug.junit.demo.Cat2BugRunnerTest\",\"key\":\"com.cat2bug.junit.demo.Cat2BugRunnerTest.testRuleFalse\"}]}";
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("${baseUrl}/api/report/defect"))
    .header("Content-Type", "application/json")
    .header("CAT2BUG-API-KEY", "${apiKey}")
    .POST(HttpRequest.BodyPublishers.ofString(body))
    .build();
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());
```
```python title=Python
import json
import urllib.request

url = "${baseUrl}/api/report/defect"
body = json.dumps({
    "handler": "demo",
    "reportTitle": "单元测试报告:com.cat2bug.junit.demo.Cat2BugRunnerTest",
    "reportTime": 1711546667143,
    "reportDescription": "报告描述",
    "reportData": [{
        "defectName": "[testRuleFalse(com.cat2bug.junit.demo.Cat2BugRunnerTest)]",
        "defectDescribe": "缺陷描述",
        "defectState": "PROCESSING",
        "defectType": "BUG",
        "defectLevel": "middle",
        "moduleVersion": "0.0.1",
        "groupKey": "com.cat2bug.junit.demo.Cat2BugRunnerTest",
        "key": "com.cat2bug.junit.demo.Cat2BugRunnerTest.testRuleFalse",
    }],
}).encode("utf-8")
req = urllib.request.Request(
    url,
    data=body,
    method="POST",
    headers={
        "Content-Type": "application/json",
        "CAT2BUG-API-KEY": "${apiKey}",
    },
)
with urllib.request.urlopen(req) as resp:
    print(resp.read().decode())
```
```javascript title=Node.js
const body = JSON.stringify({
  handler: 'demo',
  reportTitle: '单元测试报告:com.cat2bug.junit.demo.Cat2BugRunnerTest',
  reportTime: 1711546667143,
  reportDescription: '报告描述',
  reportData: [{
    defectName: '[testRuleFalse(com.cat2bug.junit.demo.Cat2BugRunnerTest)]',
    defectDescribe: '缺陷描述',
    defectState: 'PROCESSING',
    defectType: 'BUG',
    defectLevel: 'middle',
    moduleVersion: '0.0.1',
    groupKey: 'com.cat2bug.junit.demo.Cat2BugRunnerTest',
    key: 'com.cat2bug.junit.demo.Cat2BugRunnerTest.testRuleFalse',
  }],
});

const response = await fetch(`${baseUrl}/api/report/defect`, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'CAT2BUG-API-KEY': '${apiKey}',
  },
  body,
});
console.log(await response.text());
```
```php title=PHP
$ch = curl_init('${baseUrl}/api/report/defect');
curl_setopt_array($ch, [
    CURLOPT_POST => true,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => [
        'Content-Type: application/json',
        'CAT2BUG-API-KEY: ${apiKey}',
    ],
    CURLOPT_POSTFIELDS => '{"handler":"demo","reportTitle":"单元测试报告:com.cat2bug.junit.demo.Cat2BugRunnerTest","reportTime":1711546667143,"reportDescription":"报告描述","reportData":[{"defectName":"[testRuleFalse(com.cat2bug.junit.demo.Cat2BugRunnerTest)]","defectDescribe":"缺陷描述","defectState":"PROCESSING","defectType":"BUG","defectLevel":"middle","moduleVersion":"0.0.1","groupKey":"com.cat2bug.junit.demo.Cat2BugRunnerTest","key":"com.cat2bug.junit.demo.Cat2BugRunnerTest.testRuleFalse"}]}',
]);
$response = curl_exec($ch);
curl_close($ch);
echo $response;
```
```csharp title=C#
using System.Net.Http;
using System.Text;

var client = new HttpClient();
var content = new StringContent(
    "{\"handler\":\"demo\",\"reportTitle\":\"单元测试报告:com.cat2bug.junit.demo.Cat2BugRunnerTest\",\"reportTime\":1711546667143,\"reportDescription\":\"报告描述\",\"reportData\":[{\"defectName\":\"[testRuleFalse(com.cat2bug.junit.demo.Cat2BugRunnerTest)]\",\"defectDescribe\":\"缺陷描述\",\"defectState\":\"PROCESSING\",\"defectType\":\"BUG\",\"defectLevel\":\"middle\",\"moduleVersion\":\"0.0.1\",\"groupKey\":\"com.cat2bug.junit.demo.Cat2BugRunnerTest\",\"key\":\"com.cat2bug.junit.demo.Cat2BugRunnerTest.testRuleFalse\"}]}",
    Encoding.UTF8,
    "application/json");
var request = new HttpRequestMessage(HttpMethod.Post, "${baseUrl}/api/report/defect") { Content = content };
request.Headers.Add("CAT2BUG-API-KEY", "${apiKey}");
var response = await client.SendAsync(request);
Console.WriteLine(await response.Content.ReadAsStringAsync());
```
:::

::: tip 提示
请将 `${baseUrl}`、`${apiKey}` 替换为实际部署地址与项目 API KEY（参见 [api-intro.md](./api-intro.md)）。
:::

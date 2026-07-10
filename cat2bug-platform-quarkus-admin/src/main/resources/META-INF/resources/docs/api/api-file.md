# 文件接口

## 上传图片

**方法**：POST

**路径**：`/api/file/upload`

### Headers

`Content-Type`: `multipart/form-data`

### Request 请求参数

| 参数名 | 类型 | 是否必填 | 说明 |
|--------|------|:--------:|------|
| file | 文件 | 是 | 待上传文件（表单字段名 `file`） |

### Response 返回值

**返回主体说明**

| 参数名 | 类型 | 是否必返 | 说明 |
|--------|------|:--------:|------|
| code | 字符型 | 是 | 返回码，正常返回 200 |
| msg | 字符型 | 是 | 接口返回消息 |
| url | 字符型 | 是 | 带服务地址的访问 URL |
| filePath | 字符型 | 是 | 不带 IP 和端口的服务器相对路径 |
| fileExtension | 字符型 | 是 | 文件扩展名 |
| originalFilename | 字符型 | 是 | 原始文件名 |
| serverFileName | 字符型 | 是 | 保存到服务器后的文件名 |

::: code-tabs
```bash title=cURL
curl -X POST "${baseUrl}/api/file/upload" \
  -H "CAT2BUG-API-KEY: ${apiKey}" \
  -F "file=@/path/to/image.png"
```
```java title=Java
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public class UploadFileExample {
    public static void main(String[] args) throws IOException, InterruptedException {
        String baseUrl = "${baseUrl}";
        String apiKey = "${apiKey}";
        Path filePath = Path.of("/path/to/image.png");
        String boundary = "----Cat2BugBoundary" + System.currentTimeMillis();
        String fileName = filePath.getFileName().toString();
        byte[] fileBytes = Files.readAllBytes(filePath);
        String bodyStart = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n"
            + "Content-Type: application/octet-stream\r\n\r\n";
        String bodyEnd = "\r\n--" + boundary + "--\r\n";
        byte[] body = concat(bodyStart.getBytes(), fileBytes, bodyEnd.getBytes());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/file/upload"))
            .header("CAT2BUG-API-KEY", apiKey)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.statusCode());
        System.out.println(response.body());
    }

    private static byte[] concat(byte[]... parts) {
        int len = 0;
        for (byte[] p : parts) {
            len += p.length;
        }
        byte[] out = new byte[len];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }
}
```
```python title=Python
import mimetypes
import os
import uuid
from urllib.request import Request, urlopen

base_url = "${baseUrl}"
api_key = "${apiKey}"
file_path = "/path/to/image.png"

boundary = f"----Cat2Bug{uuid.uuid4().hex}"
file_name = os.path.basename(file_path)
content_type = mimetypes.guess_type(file_path)[0] or "application/octet-stream"

with open(file_path, "rb") as f:
    file_data = f.read()

body = (
    f"--{boundary}\r\n"
    f'Content-Disposition: form-data; name="file"; filename="{file_name}"\r\n'
    f"Content-Type: {content_type}\r\n\r\n"
).encode() + file_data + f"\r\n--{boundary}--\r\n".encode()

req = Request(
    f"{base_url}/api/file/upload",
    data=body,
    method="POST",
    headers={
        "CAT2BUG-API-KEY": api_key,
        "Content-Type": f"multipart/form-data; boundary={boundary}",
    },
)
with urlopen(req) as resp:
    print(resp.status)
    print(resp.read().decode())
```
```javascript title=Node.js
import fs from 'node:fs';
import { Blob } from 'node:buffer';

const baseUrl = '${baseUrl}';
const apiKey = '${apiKey}';
const filePath = '/path/to/image.png';

const blob = new Blob([fs.readFileSync(filePath)]);
const form = new FormData();
form.append('file', blob, 'image.png');

const response = await fetch(`${baseUrl}/api/file/upload`, {
  method: 'POST',
  headers: {
    'CAT2BUG-API-KEY': apiKey,
  },
  body: form,
});
console.log(response.status);
console.log(await response.text());
```
```php title=PHP
<?php
$baseUrl = '${baseUrl}';
$apiKey = '${apiKey}';
$filePath = '/path/to/image.png';

$curl = curl_init();
curl_setopt_array($curl, [
    CURLOPT_URL => $baseUrl . '/api/file/upload',
    CURLOPT_POST => true,
    CURLOPT_RETURNTRANSFER => true,
    CURLOPT_HTTPHEADER => [
        'CAT2BUG-API-KEY: ' . $apiKey,
    ],
    CURLOPT_POSTFIELDS => [
        'file' => curl_file_create($filePath, mime_content_type($filePath), basename($filePath)),
    ],
]);
$response = curl_exec($curl);
$status = curl_getinfo($curl, CURLINFO_HTTP_CODE);
curl_close($curl);

echo $status . PHP_EOL;
echo $response . PHP_EOL;
```
```csharp title=C#
using System.Net.Http;
using System.Net.Http.Headers;

var baseUrl = "${baseUrl}";
var apiKey = "${apiKey}";
var filePath = "/path/to/image.png";

using var client = new HttpClient();
using var form = new MultipartFormDataContent();
await using var stream = File.OpenRead(filePath);
var fileContent = new StreamContent(stream);
fileContent.Headers.ContentType = MediaTypeHeaderValue.Parse("application/octet-stream");
form.Add(fileContent, "file", Path.GetFileName(filePath));

using var request = new HttpRequestMessage(HttpMethod.Post, $"{baseUrl}/api/file/upload");
request.Headers.Add("CAT2BUG-API-KEY", apiKey);
request.Content = form;

using var response = await client.SendAsync(request);
Console.WriteLine((int)response.StatusCode);
Console.WriteLine(await response.Content.ReadAsStringAsync());
```
:::

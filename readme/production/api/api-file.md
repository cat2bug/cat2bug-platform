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

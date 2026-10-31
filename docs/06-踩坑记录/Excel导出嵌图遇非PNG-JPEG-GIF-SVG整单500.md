# Excel 导出嵌图遇非 PNG/JPEG/GIF/SVG 整单 500

- 日期：2026-10-31
- 涉及：后台 / 缺陷导出 / 用例导出

## 现象

缺陷「导出」接口 500；前端提示「系统接口 500 异常」「下载文件出现错误」。日志：

`IllegalArgumentException: Unsupported image format. Supported formats: PNG, JPEG, GIF, SVG`
（`org.dhatim.fastexcel.ImageType.fromBytes` → `ExcelImageEmbedSupport.embedInCell`）

常见触发：自定义图片列或系统图片列里有 WebP/BMP、或误传非图片文件。

## 根因

FastExcel 嵌图只认 PNG / JPEG / GIF / SVG。旧逻辑对任意已加载字节直接 `sheet.addImage`，一种不支持格式就抛错，整次导出失败。

## 解决办法

`ExcelImageEmbedSupport`：原生格式原样嵌入；其它格式尽量 `ImageIO` 转 PNG；仍失败则跳过该图，由调用方回退写路径文本。勿把嵌图异常冒泡成接口 500。

## 以后怎么避免

凡走 FastExcel 嵌图的导出路径，统一经 `ExcelImageEmbedSupport.embedInCell`；新增图片格式时先扩转码/跳过策略，不要假设上传文件都是 PNG。

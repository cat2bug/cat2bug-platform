---
name: gb-t-38634-overview
description: >-
  Map GB/T 38634.1–.4 to ISO/IEC/IEEE 29119-1–4; reading protocol and GB OCR
  status. Use before authoring national-standard report templates.
---

# GB/T 38634 系列总览（对照 ISO 29119）

## 系列对应关系

| 国标 | 英文对应 | 主题 | 本仓库主要源 |
|------|----------|------|----------------|
| GB/T 38634.1 | ISO/IEC/IEEE 29119-1 | 概念与定义 | `_sources/ocr/gb-t-38634-1/`（见 `manifest.json`） |
| GB/T 38634.2 | ISO/IEC/IEEE 29119-2 | 测试过程 | `_sources/ocr/gb-t-38634-2/` |
| GB/T 38634.3 | ISO/IEC/IEEE 29119-3 | 测试文档 | GB OCR：`_sources/ocr/gb-t-38634-3/`；ISO 切片：`_sources/iso-29119-3/` |
| GB/T 38634.4 | ISO/IEC/IEEE 29119-4 | 测试技术 | `_sources/ocr/gb-t-38634-4/` |

国标中文规范用语以 OCR 切片为准（扫描件 OCR 有噪声，关键 shall 可与 ISO 29119-3 交叉验证）。

写 skill / 模版时：

- **结构 / shall·should·may / 信息项边界**可同时对照 ISO 29119-3 切片。
- 引用 GB 原文措辞时打开对应 OCR 切片（路径 + 页码），勿整册灌入上下文。

## 写模版时先读哪份 skill

| 任务 | 读 |
|------|-----|
| 文档类型、公共页眉、符合性 | `test-documentation-29119-3.md` |
| 概念 / 过程 / 技术（.1/.2/.4）短摘要 | `test-process-concepts-techniques.md` |
| 模版拆分·合并 | `../report-templates/taxonomy.md` |
| DSL | `../cat2bug-report-dsl/SKILL.md`（勿在本目录发明语法） |

## 分段读取协议

1. 先读本文件与各册 `manifest.json`（ISO：`_sources/iso-29119-3/manifest.json`；GB：计划路径 `_sources/ocr/gb-t-38634-{1,2,3,4}/`）。
2. 再只打开当前任务相关的 1～N 个切片；单次不超过约一章。
3. **禁止**一次加载全部四本 GB 或整本 ISO 全文。
4. Skills 只保留摘要；细节用路径 + PDF 页码回指 `_sources`。

## 模版边界原则（D1）

- 跟从 GB/T 38634.3 / ISO 29119-3 对**文档与信息项**的定义及 full / tailored 符合性。
- **禁止**机械「一类文档名 = 必须单独一个模版文件」。
- 同一文档内的必选章节落在同一主模版（或标明的附录）；独立交付物才分模版。

## GB OCR 状态

| 路径 | 状态 |
|------|------|
| `_sources/iso-29119-3/` | 已切片（见 `manifest.json`） |
| `_sources/ocr/gb-t-38634-1/` | 已 OCR + 近似章切片（53 页） |
| `_sources/ocr/gb-t-38634-2/` | 已 OCR + 近似章切片（54 页） |
| `_sources/ocr/gb-t-38634-3/` | 已 OCR + 近似章切片（111 页）；目录/符合性抽检可读 |
| `_sources/ocr/gb-t-38634-4/` | 已 OCR + 近似章切片（127 页） |

章边界为近似值，可按目录页继续收紧 `manifest.json` 的 `pageStart`/`pageEnd`。重跑工具：`scripts/ocr-gb-volume.py`。

# API 接口说明

## API 授权配置

第三方访问 API 接口数据需要先在 Cat2Bug-Platform 系统配置授权 API_KEY（注意：每个项目需要单独配置 API_KEY，API_KEY 和项目是一对一的关系）。

### 配置步骤

1. 点击项目中的「项目设置」->「API KEY」选项，进入 API 授权页面；

![选择Api Key](./images/api/api_key_link.png)

2. 点击右侧「新建Key」按钮打开「添加项目API」对话框创建新的授权信息；

![创建Api Key](./images/api/api_key_add.png)

3. 创建保存后，可以在「API KEY」列表中查看授权码；

![查看Api Key](./images/api/api_key_list.png)

## 接口调用

第三方系统调用 Cat2Bug-Platform 接口时，需要在接口请求头部添加之前创建的「API KEY」，变量名为 `CAT2BUG-API-KEY`：

**项目范围**：`CAT2BUG-API-KEY` 与项目是绑定的，Open API 请求**不需要**也不应再传 `projectId`；各子文档中的请求参数均不包含项目 ID。

**名称化参数**：测试用例、交付物等接口使用**用例编号**、**交付物全路径**（多级用 `/` 分隔）等与界面一致的可读标识；请勿传 `caseId`、`moduleId`、`deliverableId` 等数字主键（响应 JSON 中亦不返回此类字段，详见各子文档）。

### 服务地址

示例代码中的 `${baseUrl}` 为**不含末尾斜杠**的 Open API 根地址。按当前仓库默认开发配置，取值如下（第三方系统、脚本联调请用「直连后端」一行；与浏览器里前台页面同源访问时，与前台请求一致，用「经前台调用 API」一行）：

| 部署场景 | 地址 |
|----------|------|
| 直连后端（Open API / 脚本推荐） | `${baseUrlDirect}` |
| 经前台调用 API（与浏览器访问前台一致） | `${baseUrl}` |

完整请求 URL 写法：`${baseUrl}/api/***`（不要在 `${baseUrl}` 后多加 `/` 再拼路径）。

## 接口列表

系统提供以下业务接口：

### 交付物接口
- [查看交付物树形列表](api-deliverable.md#查看交付物树形列表)
- [查看交付物详情](api-deliverable.md#查看交付物详情)
- [创建交付物](api-deliverable.md#创建交付物)

### 测试用例接口
- [查看测试用例列表](api-case.md#查看测试用例列表)
- [查看测试用例详情](api-case.md#查看测试用例详情)
- [创建测试用例](api-case.md#创建测试用例)
- [更新测试用例](api-case.md#更新测试用例)
- [删除测试用例](api-case.md#删除测试用例)

### 缺陷接口
- [查看缺陷列表](api-defect.md#查看缺陷列表)
- [创建缺陷](api-defect.md#创建缺陷)
- [修改缺陷](api-defect.md#修改缺陷)
- [查看缺陷详情](api-defect.md#查看缺陷详情)
- [修复缺陷](api-defect.md#修复缺陷)
- [通过缺陷](api-defect.md#通过缺陷)
- [驳回缺陷](api-defect.md#驳回缺陷)
- [指派缺陷](api-defect.md#指派缺陷)
- [关闭缺陷](api-defect.md#关闭缺陷)
- [开启缺陷](api-defect.md#开启缺陷)

### 报告接口
- [推送缺陷报告](api-report-defect.md)

### 成员接口
- [查看项目成员列表](api-member.md#查看项目成员列表)

### 文件接口
- [上传图片](api-file.md#上传图片)

### 项目接口
- [获取项目信息](api-project.md#获取项目信息)

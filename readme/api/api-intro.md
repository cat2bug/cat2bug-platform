# API 接口说明

## API 授权配置

第三方访问 API 接口数据需要先在 Cat2Bug-Platform 系统配置授权 API_KEY（注意：每个项目需要单独配置 API_KEY，API_KEY 和项目是一对一的关系）。

### 配置步骤

1. 点击项目中的【项目设置】->【API KEY】选项，进入 API 授权页面；

![选择Api Key](./images/api/api_key_link.png)

2. 点击右侧【新建Key】按钮打开【添加项目API】对话框创建新的授权信息；

![创建Api Key](./images/api/api_key_add.png)

3. 创建保存后，可以在【API KEY】列表中查看授权码；

![查看Api Key](./images/api/api_key_list.png)

## 接口调用

第三方系统调用 Cat2Bug-Platform 接口时，需要在接口请求头部添加之前创建的【API KEY】，变量名为 `CAT2BUG-API-KEY`，如下示例：

```bash
curl --location -X POST 'http://localhost:2020/api/defect' \
-H 'Content-Type: application/json' \
-H 'CAT2BUG-API-KEY: 202411011547433W484U00XIEXL3U6' \
-d '{"defectType": 1, "defectName":"defectName", "defectDescribe":"defectDescribe", "defectLevel":2 }'
```

## 接口列表

系统提供以下业务接口：

### 交付物接口
- [查看交付物树形列表](./api-deliverable.md#查看交付物树形列表)
- [查看交付物详情](./api-deliverable.md#查看交付物详情)
- [创建交付物](./api-deliverable.md#创建交付物)

### 测试用例接口
- [查看测试用例列表](./api-case.md#查看测试用例列表)
- [查看测试用例详情](./api-case.md#查看测试用例详情)
- [创建测试用例](./api-case.md#创建测试用例)
- [更新测试用例](./api-case.md#更新测试用例)
- [删除测试用例](./api-case.md#删除测试用例)

### 缺陷接口
- [查看缺陷列表](./api-defect.md#查看缺陷列表)
- [创建缺陷](./api-defect.md#创建缺陷)
- [修改缺陷](./api-defect.md#修改缺陷)
- [查看缺陷详情](./api-defect.md#查看缺陷详情)
- [修复缺陷](./api-defect.md#修复缺陷)
- [通过缺陷](./api-defect.md#通过缺陷)
- [驳回缺陷](./api-defect.md#驳回缺陷)
- [指派缺陷](./api-defect.md#指派缺陷)
- [关闭缺陷](./api-defect.md#关闭缺陷)
- [开启缺陷](./api-defect.md#开启缺陷)

### 报告接口
- [推送缺陷报告](./api-report-defect.md)

### 成员接口
- [查看项目成员列表](./api-member.md#查看项目成员列表)

### 文件接口
- [上传图片](./api-file.md#上传图片)

### 项目接口
- [获取项目信息](./api-project.md#获取项目信息)

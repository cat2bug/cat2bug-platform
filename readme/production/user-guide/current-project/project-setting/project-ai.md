# AI管理 [/project/project-ai](/project/project-ai)

## 概述

AI管理是项目管理员用于配置项目AI功能的模块。通过AI管理，可以配置AI大模型服务和OpenAI账号，为项目提供智能化的辅助功能。

![项目设置页面](../../../images/user-guide/current-project/project-setting/01-project-setting-overview.png)

## 功能说明

### AI大模型 [/project/project-ai](/project/project-ai)

配置项目使用的AI大模型服务。

**功能：**
- 选择AI模型类型（OpenAI、Claude、国产大模型等）
- 配置模型参数
- 测试模型连接

**操作步骤：**
1. 点击"AI大模型"
2. 选择要使用的AI模型
3. 配置模型参数（API地址、模型版本等）
4. 点击"测试连接"验证配置
5. 保存配置

**支持的AI模型：**
- OpenAI GPT系列
- Anthropic Claude系列
- 国产大模型（文心一言、通义千问等）
- 自定义API接口

### OpenAI账号管理 [/system/project/ai/account](/system/project/ai/account)

管理项目的OpenAI账号配置。

**功能：**
- 添加OpenAI API密钥
- 配置组织ID
- 设置使用额度
- 查看使用统计

**操作步骤：**
1. 点击"OpenAI账号管理"
2. 输入API密钥
3. 配置相关参数（组织ID、使用限额等）
4. 保存配置

**配置项说明：**
- **API Key**：OpenAI提供的访问密钥
- **Organization ID**：OpenAI组织标识（可选）
- **使用限额**：设置每月最大使用额度
- **默认模型**：选择默认使用的GPT模型版本

## AI功能应用场景

配置AI后，可以在以下场景中使用：

- **缺陷描述生成**：根据简要信息自动生成详细的缺陷描述
- **测试用例生成**：根据需求自动生成测试用例
- **代码分析**：分析代码片段，识别潜在问题
- **智能问答**：回答项目相关的技术问题

## 权限说明

只有项目管理员才能配置AI管理设置。

## 常见问题

**Q: AI功能需要额外付费吗？**  
A: 使用OpenAI等第三方AI服务需要自行购买API密钥，系统本身不收取AI功能费用。

**Q: 如何获取OpenAI API密钥？**  
A: 访问OpenAI官网（https://platform.openai.com），注册账号后在API Keys页面创建密钥。

**Q: 配置多个AI模型可以吗？**  
A: 可以。系统支持配置多个AI模型，可以根据不同场景选择使用。

**Q: AI模型连接测试失败怎么办？**  
A: 请检查API密钥是否正确、网络连接是否正常、API地址是否可访问。

**Q: 使用AI功能会泄露项目数据吗？**  
A: AI服务提供商可能会记录API请求内容，建议不要在敏感项目中使用，或选择支持私有部署的AI模型。
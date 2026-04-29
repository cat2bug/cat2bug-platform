# 创建报告

通过 Open API 创建测试报告，自动化生成标准化的测试报告。

## 使用场景

- 自动化生成测试报告
- 集成到 CI/CD 流程
- 定期生成测试报告
- 批量生成多个报告

## 创建方式

### 通过 Open API 创建

目前报告主要通过 Open API 方式创建，可以自动化生成测试报告。

#### API 调用示例

```bash
POST /api/report/create
Content-Type: application/json
X-API-Key: your-api-key

{
  "projectId": 1,
  "reportName": "V1.0 版本测试报告",
  "reportType": "功能测试报告",
  "content": "# 测试报告内容（Markdown格式）",
  "startTime": "2024-01-01",
  "endTime": "2024-01-15"
}
```

#### 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | Integer | 是 | 项目 ID |
| reportName | String | 是 | 报告标题 |
| reportType | String | 是 | 报告类型 |
| content | String | 是 | 报告内容（Markdown 格式） |
| startTime | String | 否 | 测试开始时间 |
| endTime | String | 否 | 测试结束时间 |

#### 响应示例

```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "reportId": 123,
    "reportName": "V1.0 版本测试报告",
    "createTime": "2024-01-15 10:00:00"
  }
}
```

### 手动创建（规划中）

未来版本将支持在界面上手动创建和编辑报告，提供更友好的编辑体验。

**规划功能：**
- 可视化报告编辑器
- 报告模板选择
- 自动收集测试数据
- 实时预览报告效果

## 报告内容编写

### Markdown 格式

报告内容使用 Markdown 格式编写，支持丰富的格式。

**支持的 Markdown 语法：**

```markdown
# 一级标题
## 二级标题
### 三级标题

**粗体文本**
*斜体文本*

- 无序列表项 1
- 无序列表项 2

1. 有序列表项 1
2. 有序列表项 2

[链接文本](链接地址)

![图片描述](图片地址)

| 表头1 | 表头2 |
|-------|-------|
| 内容1 | 内容2 |

`行内代码`

​```
代码块
​```
```

### 报告结构建议

建议按以下结构组织报告内容：

```markdown
# 报告标题

## 1. 测试概述
- 测试目标
- 测试范围
- 测试时间
- 测试人员
- 测试环境

## 2. 测试执行情况
- 用例统计
- 执行率和通过率
- 执行过程说明

## 3. 缺陷统计
- 缺陷总览
- 各状态缺陷分布
- 各优先级缺陷分布
- 各类型缺陷分布

## 4. 模块质量分析
- 各交付物统计
- 高风险模块识别
- 质量趋势分析

## 5. 测试结论
- 测试完成情况
- 质量评估
- 遗留问题
- 发布建议
- 风险提示

## 6. 改进建议
- 测试过程改进
- 产品质量改进
- 团队协作改进
```

### 数据收集

通过 API 收集测试数据，用于生成报告。

**可收集的数据：**
- 测试用例统计数据
- 缺陷统计数据
- 交付物统计数据
- 测试计划执行数据

**数据收集示例：**

```python
import requests

# 获取用例统计
case_stats = requests.get(
    'https://api.cat2bug.com/api/case/statistics',
    headers={'X-API-Key': 'your-api-key'},
    params={'projectId': 1}
).json()

# 获取缺陷统计
defect_stats = requests.get(
    'https://api.cat2bug.com/api/defect/statistics',
    headers={'X-API-Key': 'your-api-key'},
    params={'projectId': 1}
).json()

# 生成报告内容
content = f"""
# 测试报告

## 测试执行情况
- 用例总数：{case_stats['total']}
- 已执行：{case_stats['executed']}
- 通过：{case_stats['passed']}

## 缺陷统计
- 缺陷总数：{defect_stats['total']}
- 待处理：{defect_stats['open']}
- 已关闭：{defect_stats['closed']}
"""

# 创建报告
response = requests.post(
    'https://api.cat2bug.com/api/report/create',
    headers={'X-API-Key': 'your-api-key'},
    json={
        'projectId': 1,
        'reportName': 'V1.0 版本测试报告',
        'reportType': '功能测试报告',
        'content': content
    }
)
```

## 报告模板

系统提供了多种报告模板，可以快速生成标准化的测试报告。

### 功能测试报告模板

适用于功能测试，包含：
- 功能测试概述
- 用例执行情况
- 功能缺陷统计
- 功能质量评估

### 性能测试报告模板

适用于性能测试，包含：
- 性能测试场景
- 性能指标统计
- 性能瓶颈分析
- 性能优化建议

### 安全测试报告模板

适用于安全测试，包含：
- 安全测试范围
- 安全漏洞统计
- 风险等级评估
- 安全加固建议

### 验收测试报告模板

适用于项目验收，包含：
- 验收标准
- 验收测试结果
- 遗留问题清单
- 验收结论

## 自动化生成

### 定时生成

可以设置定时任务，自动生成测试报告。

**定时任务示例：**

```python
from apscheduler.schedulers.blocking import BlockingScheduler

def generate_report():
    # 收集数据
    # 生成报告内容
    # 调用 API 创建报告
    pass

scheduler = BlockingScheduler()
# 每周五下午 5 点生成报告
scheduler.add_job(generate_report, 'cron', day_of_week='fri', hour=17)
scheduler.start()
```

### CI/CD 集成

将报告生成集成到 CI/CD 流程中。

**Jenkins 集成示例：**

```groovy
stage('Generate Test Report') {
    steps {
        script {
            sh 'python generate_report.py'
        }
    }
}
```

**GitLab CI 集成示例：**

```yaml
generate_report:
  stage: report
  script:
    - python generate_report.py
  only:
    - master
```

## API 文档

详细的 API 文档请参考 [报告接口文档](../../api/api-report-defect.md)。

::: tip 提示
1. 报告内容必须使用 Markdown 格式
2. 建议使用报告模板，保持格式统一
3. 可以通过 API 自动收集测试数据
4. 支持集成到 CI/CD 流程自动生成
5. 未来版本将支持界面手动创建报告
:::

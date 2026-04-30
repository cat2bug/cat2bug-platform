# 通过 API 创建报告

通过 Open API 自动化创建测试报告，实现报告生成的自动化和集成。

## 使用场景

- 自动化生成测试报告
- 集成到 CI/CD 流程
- 定期生成测试报告
- 批量推送缺陷数据

## 推送缺陷报告

### 接口说明

推送缺陷报告接口用于自动化提交测试结果和缺陷数据。

**接口路径：** `/api/report/defect`

**请求方法：** POST

**请求头：**
- `Content-Type: application/json`
- `CAT2BUG-API-KEY: your-api-key`

### 请求示例

```bash
curl --location -X POST 'https://www.cat2bug.com:8022/prod-api/api/report/defect' \
-H 'Content-Type: application/json' \
-H 'CAT2BUG-API-KEY: 20240327001413he1mbyfi6fhnfets' \
-d '{
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
      "moduleVersion": "0.0.1",
      "groupKey": "com.cat2bug.junit.demo.Cat2BugRunnerTest",
      "key": "com.cat2bug.junit.demo.Cat2BugRunnerTest.testRuleFalse"
    }
  ]
}'
```

### 请求参数说明

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| handler | String | 否 | 处理人，0至30个字节 |
| reportTitle | String | 是 | 报告标题，1至255个字节 |
| reportTime | Long | 是 | 报告时间（时间戳） |
| reportDescription | String | 是 | 报告描述，1至65536个字节 |
| reportData | Array | 否 | 缺陷数组 |

### 缺陷数据参数说明

reportData 数组中每个缺陷对象的属性：

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| defectName | String | 是 | 缺陷名称，1至128个字节 |
| defectDescribe | String | 是 | 缺陷描述，1至65536个字节 |
| defectState | String | 否 | 缺陷状态，默认PROCESSING<br>PROCESSING(待处理)、AUDIT(待审核)、REJECTED(已驳回)、CLOSED(已关闭) |
| defectType | String | 否 | 缺陷类型，默认BUG<br>BUG(错误)、TASK(任务)、DEMAND(需求) |
| defectLevel | String | 否 | 缺陷级别，默认MIDDLE<br>LOW(低)、MIDDLE(中)、HEIGHT(高)、URGENT(急) |
| moduleVersion | String | 否 | 交付物的版本号，1至128个字节 |
| groupKey | String | 否 | 缺陷唯一关键组，1至255个字节<br>通过此属性查找相同组的缺陷 |
| key | String | 否 | 缺陷唯一关键字，1至255个字节 |

### 响应示例

```json
{
  "msg": "操作成功",
  "code": 200
}
```

**说明：** code 返回 200 代表推送成功。

## 使用说明

### groupKey 和 key 的作用

**groupKey（缺陷唯一关键组）：**
- 用于标识一组相关的缺陷
- 例如：将某个类的类名作为 groupKey
- 当测试后没有发现此类有问题时，可通过 groupKey 批量更新所有相关缺陷的状态

**key（缺陷唯一关键字）：**
- 用于标识单个缺陷的唯一性
- 避免重复创建相同的缺陷
- 例如：使用"类名.方法名"作为唯一标识

### 使用示例

**Python 示例：**

```python
import requests
import time

def push_defect_report(api_key, handler, title, description, defects):
    """推送缺陷报告"""
    url = 'https://www.cat2bug.com:8022/prod-api/api/report/defect'
    headers = {
        'Content-Type': 'application/json',
        'CAT2BUG-API-KEY': api_key
    }
    data = {
        'handler': handler,
        'reportTitle': title,
        'reportTime': int(time.time() * 1000),
        'reportDescription': description,
        'reportData': defects
    }
    
    response = requests.post(url, headers=headers, json=data)
    return response.json()

# 使用示例
api_key = 'your-api-key'
defects = [
    {
        'defectName': '登录功能测试失败',
        'defectDescribe': '用户名为空时未提示错误信息',
        'defectState': 'PROCESSING',
        'defectType': 'BUG',
        'defectLevel': 'HEIGHT',
        'moduleVersion': '1.0.0',
        'groupKey': 'LoginModule',
        'key': 'LoginModule.EmptyUsername'
    },
    {
        'defectName': '密码验证失败',
        'defectDescribe': '密码长度小于6位时未提示',
        'defectState': 'PROCESSING',
        'defectType': 'BUG',
        'defectLevel': 'MIDDLE',
        'moduleVersion': '1.0.0',
        'groupKey': 'LoginModule',
        'key': 'LoginModule.PasswordLength'
    }
]

result = push_defect_report(
    api_key=api_key,
    handler='tester01',
    title='登录模块测试报告',
    description='登录模块功能测试，发现2个缺陷',
    defects=defects
)
print(result)
```

**Java 示例：**

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.*;

public class DefectReportClient {
    
    public static void pushDefectReport(String apiKey, String handler, 
                                       String title, String description, 
                                       List<Map<String, Object>> defects) throws Exception {
        String url = "https://www.cat2bug.com:8022/prod-api/api/report/defect";
        
        Map<String, Object> data = new HashMap<>();
        data.put("handler", handler);
        data.put("reportTitle", title);
        data.put("reportTime", System.currentTimeMillis());
        data.put("reportDescription", description);
        data.put("reportData", defects);
        
        ObjectMapper mapper = new ObjectMapper();
        String jsonData = mapper.writeValueAsString(data);
        
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .header("CAT2BUG-API-KEY", apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(jsonData))
            .build();
        
        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());
    }
    
    public static void main(String[] args) throws Exception {
        String apiKey = "your-api-key";
        
        List<Map<String, Object>> defects = new ArrayList<>();
        Map<String, Object> defect = new HashMap<>();
        defect.put("defectName", "登录功能测试失败");
        defect.put("defectDescribe", "用户名为空时未提示错误信息");
        defect.put("defectState", "PROCESSING");
        defect.put("defectType", "BUG");
        defect.put("defectLevel", "HEIGHT");
        defect.put("moduleVersion", "1.0.0");
        defect.put("groupKey", "LoginModule");
        defect.put("key", "LoginModule.EmptyUsername");
        defects.add(defect);
        
        pushDefectReport(apiKey, "tester01", "登录模块测试报告", 
                        "登录模块功能测试", defects);
    }
}
```

## 自动化集成

### 定时生成

可以设置定时任务，自动推送测试报告。

**定时任务示例：**

```python
from apscheduler.schedulers.blocking import BlockingScheduler
import requests
import time

def generate_and_push_report():
    """生成并推送测试报告"""
    # 收集测试数据
    defects = collect_test_defects()
    
    # 推送报告
    api_key = 'your-api-key'
    url = 'https://www.cat2bug.com:8022/prod-api/api/report/defect'
    headers = {
        'Content-Type': 'application/json',
        'CAT2BUG-API-KEY': api_key
    }
    data = {
        'handler': 'auto-tester',
        'reportTitle': f'自动化测试报告 - {time.strftime("%Y-%m-%d %H:%M:%S")}',
        'reportTime': int(time.time() * 1000),
        'reportDescription': '自动化测试执行结果',
        'reportData': defects
    }
    
    response = requests.post(url, headers=headers, json=data)
    print(f'报告推送结果: {response.json()}')

def collect_test_defects():
    """收集测试缺陷数据"""
    # 这里实现你的测试逻辑
    return []

scheduler = BlockingScheduler()
# 每周五下午 5 点生成报告
scheduler.add_job(generate_and_push_report, 'cron', day_of_week='fri', hour=17)
scheduler.start()
```

### CI/CD 集成

将报告推送集成到 CI/CD 流程中。

**Jenkins 集成示例：**

```groovy
stage('Push Test Report') {
    steps {
        script {
            sh 'python push_defect_report.py'
        }
    }
}
```

**GitLab CI 集成示例：**

```yaml
push_report:
  stage: report
  script:
    - python push_defect_report.py
  only:
    - master
```

**GitHub Actions 集成示例：**

```yaml
name: Push Test Report

on:
  push:
    branches: [ main ]
  schedule:
    - cron: '0 17 * * 5'  # 每周五下午5点

jobs:
  test-and-report:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run tests
        run: pytest
      - name: Push defect report
        env:
          CAT2BUG_API_KEY: ${{ secrets.CAT2BUG_API_KEY }}
        run: python push_defect_report.py
```

## 最佳实践

### 报告标题命名

建议使用清晰的命名规范：
- 包含测试类型：单元测试、集成测试、功能测试等
- 包含模块名称：登录模块、支付模块等
- 包含时间信息：日期或版本号

**示例：**
- ✅ 单元测试报告:登录模块-2024-01-15
- ✅ 集成测试报告:支付流程-v1.2.0
- ✅ 自动化测试报告:API接口-每日构建
- ❌ 测试报告
- ❌ report1

### 缺陷描述规范

**defectName（缺陷名称）：**
- 简洁明了，一句话说明问题
- 包含关键信息：功能点、问题现象

**defectDescribe（缺陷描述）：**
- 详细描述问题
- 包含复现步骤
- 包含预期结果和实际结果
- 可以包含错误日志

**示例：**

```json
{
  "defectName": "登录接口-用户名为空时返回500错误",
  "defectDescribe": "复现步骤：\n1. 调用登录接口\n2. 用户名参数传空字符串\n3. 密码参数正常\n\n预期结果：返回400错误，提示用户名不能为空\n实际结果：返回500错误\n\n错误日志：\nNullPointerException at UserService.login()"
}
```

### 缺陷级别设置

根据影响程度合理设置缺陷级别：

- **URGENT（急）**：系统崩溃、数据丢失、安全漏洞
- **HEIGHT（高）**：核心功能无法使用、严重影响用户体验
- **MIDDLE（中）**：功能可用但有明显问题、影响部分用户
- **LOW（低）**：界面问题、提示信息不准确、优化建议

### 使用 groupKey 管理缺陷

合理使用 groupKey 可以批量管理相关缺陷：

```python
# 第一次推送：发现登录模块的多个缺陷
defects = [
    {
        'defectName': '用户名验证失败',
        'defectDescribe': '...',
        'groupKey': 'LoginModule',
        'key': 'LoginModule.UsernameValidation'
    },
    {
        'defectName': '密码验证失败',
        'defectDescribe': '...',
        'groupKey': 'LoginModule',
        'key': 'LoginModule.PasswordValidation'
    }
]

# 第二次推送：登录模块修复后，没有发现新问题
# 可以通过 groupKey 批量关闭之前的缺陷
defects = []  # 空数组表示没有新缺陷
# 系统会自动处理 groupKey='LoginModule' 的历史缺陷
```

## 相关文档

- [报告接口文档](../../../api/api-report-defect.md) - 完整的 API 接口说明
- [通过模版创建报告](report-create-template.md) - 手动创建报告的方式
- [查看报告](report-view.md) - 查看已创建的报告

::: tip 提示
1. 请妥善保管 API Key，不要泄露给他人
2. 报告时间使用毫秒级时间戳
3. 缺陷描述支持换行符 `\n`
4. 合理使用 groupKey 和 key 避免重复创建缺陷
5. 建议在测试框架中集成此接口，实现自动化报告推送
:::


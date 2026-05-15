# 导入用例

和其它传统缺陷管理系统相同，我们提供了从 Excel 导入测试用例的功能，此方式主要考虑到用户可以从一些正在进行的项目中快速转移项目数据。

## 1. 下载测试用例模版

我们提供了一套标准的测试用例模版格式，点击「导入用例」对话框中的「下载模版」链接按钮，即可下载 Excel 模版文件。

值得注意的是，此模版中交付物选项是非必填的（系统中，测试用例的交付物属性是必填项），此处主要考虑到在没有完全维护好交付物结构的时候，也可以畅通无阻的完成测试用例的导入工作。操作如下图：

![导入用例模版](../../../images/user-guide/current-project/case/case_import_button.png)

![导入用例模版](../../../images/user-guide/current-project/case/case_import_template.png)

## 2. 在 Excel 模版中录入数据

在 Excel 模版中，红色标题的列是必填项；交付物、用例级别等是下拉选项，目前 Excel 最大支持 65536 条数据的录入。

值得一提的是，导入的「步骤」属性，格式规则如下：

- 一行算一个步骤
- 步骤的描述与预期用 `---` 分割

**示例：**
```
输入用户名---正常录入
输入用户密码---正常录入
点击"登录"按钮---跳转到首页
```

![导入用例的Excel](../../../images/user-guide/current-project/case/case_import_excel.png)

## 3. 导入数据

将维护好的 Excel 文件导入到系统，如录入的数据无误，系统将提示导入成功，如下图：

![导入用例](../../../images/user-guide/current-project/case/case_import.png)

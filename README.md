<div align="center"><img src="readme/images/logo.png" style="width: 300px;" /></div>
<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">Cat2Bug-Platform v0.6.1</h1>
<h4 align="center">轻量化智能BUG管理平台</h4>

## 平台简介

Cat2Bug-Platform是一套永久免费开源的Bug管理平台，可以完全私有化部署，它利用目前比较流行的AI大数据模型技术作为辅助，快速提升软件管理的质量，我们将毫无保留给个人及团体免费使用。
它的使用人群锁定个人或中小型软件开发团队，Cat2Bug的理念是免去了项目管理中各种重度管理，让个人或团队可以快速上手，把控软件质量。
平台采用JAVA+VUE前后台分离模式开发，支持在各系统平台部署使用。

## 内置功能

1.  仪表盘：统计缺陷、成员、计划信息。
2.  团队管理：管理团队中的项目、成员。
3.  项目管理：管理项目中的缺陷、成员。
4.  用例管理：管理测试用例
5.  缺陷管理：管理BUG、需求、任务。
6.  交付物管理：维护项目中的可交付物品。
7.  报告管理：显示团队、项目、测试用例、缺陷、交付物等的相关数据指标。
8.  API管理：用于管理API接口密钥
9.  文档管理：留备项目中所用到的各种文档
10.  通知管理：发送系统业务通知到系统内部、邮件、钉钉等平台中。

## 最新版本更新说明

当前最新版本是0.6.1

* 交付物中添加附件;
* 优化页面打开慢的问题;
* 接口支持Cat2Bug-App微信小程序;
* 添加OpenAI接口，用于测试用例自动生成;
* 添加数据库自动升级功能;
* 修复系统功能BUG;

## 特色

* 开源私有化AI+BUG系统部署;
* 通过AI技术自动生成测试用例并录入到系统，解决费时费力录入用例的痛点;
* 已测试平台为生态中心，衍生多种缺陷监控测试框架，可以一站式解决软件生产运维中的诸多痛点；
* 自主研发报告模版，可轻松、快速、动态的生成项目所需管理及交付文档，较免管理人员编写文档的时间成本;
* 专注于软件的缺陷的跟踪管理，简单直接，即开即用，减少学习成本；

## 在线体验

- 体验账号：demo
- 体验密码：123456

演示地址：[https://www.cat2bug.com:8022](https://www.cat2bug.com:8022)

## 关联产品

| 名称                                                       | 类型       | 说明                                                                |
|----------------------------------------------------------|----------|:------------------------------------------------------------------|
| [Cat2Bug-App](https://gitee.com/cat2bug/cat2bug-app)     | BUG手机端   | Cat2Bug-Platform平台的手机端系统                                          |
| [Cat2Bug-JUnit](https://gitee.com/cat2bug/cat2bug-junit) | 单元测试框架   | 自动化单元测试框架，目前可以自动扫描Controller接口，随机提供参数测试，并将测试报告提交到Cat2Bug-Platform |
| [Cat2Bug-JLog](https://gitee.com/cat2bug/cat2bug-jlog)   | 错误日志采集框架 | 获取项目中的异常日志，并将日志报告提交到Cat2Bug-Platform                              |

## 系统架构

![系统架构](readme/images/cat2bug-platform-framework.png)

## 技术选型

1. 系统环境

* Java EE 11
* Servlet 3.0
* Apache Maven 3

2. 主框架

* Spring Boot 2.2.x
* Spring Framework 5.2.x
* Spring Security 5.2.x

3. 持久层

* Apache MyBatis 3.5.x
* Hibernate Validation 6.0.x
* Alibaba Druid 1.2.x

4. 视图层

* Vue 2.6.x
* Npm 16.16.0
* Axios 0.21.x
* Element 2.15.x

## 模块

````
--cat2bug-platform
------|----cat2bug-platform-admin       # 主程序模块
------|----cat2bug-platform-ai          # 人工智能模块
------|----cat2bug-platform-im          # 通讯模块
------|----cat2bug-platform-api         # Open API模块
------|----cat2bug-platform-common      # 通用模块
------|----cat2bug-platform-framework   # 系统框架
------|----cat2bug-platform-generator   # 代码生成
------|----cat2bug-platform-quartz      # 定时任务
------|----cat2bug-platform-system      # 业务模块
------|----cat2bug-platform-ui          # 前端VUE工程
------|----sql                          # 数据库文件
------|----readme                       # 文档
````

## 部署

### 手动命令行部署

手动部署需要提前安装Java 11环境，并下载cat2bug-platform发行版程序，执行命令如下：

```shell
nohup java -jar cat2bug-platform-0.6.1.jar>/dev/null 2>&1 &
```

### Docker单容器部署

以下提供的是Docker官网容器化的部署方案，执行命令如下：

```docker
docker run -it -d -p 8022:8022 --name cat2bug-platform qyzw-docker.pkg.coding.net/cat2bug/cat2bug-platform/single:latest
```

部署成功后，打开浏览器访问[http://127.0.0.1:8022](http://127.0.0.1:8022),在登陆页面自行注册账号登陆使用即可。

注意：系统管理员账号：admin    密码：cat2bug，此账号用于管理注册用户。

此部署方式为单容器最精简方式部署，数据库默认采用嵌入式H2，多用于小型或临时性项目的缺陷管理，如需Mysql或多容器部署方案，请查看[官网文档](https://www.cat2bug.com/download/cat2bug-platform/#%E9%83%A8%E7%BD%B2)。

## 演示图

<table>
    <tr>
        <td><img src="readme/images/1.png"></td>
        <td><img src="readme/images/2.png"></td>
    </tr>
    <tr>
        <td><img src="readme/images/3.png"></td>
        <td><img src="readme/images/4.png"></td>
    </tr>
    <tr>
        <td><img src="readme/images/5.png"></td>
        <td><img src="readme/images/6.png"></td>
    </tr>
    <tr>
        <td><img src="readme/images/7.png"></td>
        <td><img src="readme/images/8.png"></td>
    </tr>
</table>

## 未来计划

目前Cat2Bug还在持续成长中，后续我们将在测试工具、自动化、AI几个方向持续投入，完善平台的功能。2026计划如下：

* cat2bug-platform: 功能叠加，完善系统统计管理功能；
* cat2bug-app：完善移动端APP；
* cat2bug-cloud：cat2bug云平台的建设；

## Cat2Bug交流群

| QQ群： [731462000](https://qm.qq.com/cgi-bin/qm/qr?k=G_vJa478flcFo_1ohJxNYD0mRKafQ7I1&jump_from=webapi&authKey=EL0KrLpnjYWqNN9YXTVksNlNFrV9DHYyPMx2RVOhXqLzfnmc+Oz8oQ38aBOGx90t) | 微信群：Cat2Bug                                                                 |
|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------|
| <img src="./readme/images/qq.png" style="width: 150px; height: 150px;">                                                                                                        | <img src="./readme/images/wechat.png" style="width: 150px; height: 150px;"> |

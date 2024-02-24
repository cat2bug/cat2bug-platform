<h1 align="center" style="margin: 30px 0 30px; font-weight: bold;">Cat2Bug-Platform v0.2.0</h1>
<h4 align="center">轻量化BUG管理平台</h4>

## 平台简介

Cat2Bug-Platform是一套永久免费开源的Bug管理平台，将毫无保留给个人及团体免费使用。
它的使用人群锁定个人或中小型软件开发团队，Cat2Bug的理念是免去了项目管理中各种重度管理，让个人或团队可以快速上手，把控软件质量。
平台采用JAVA+VUE开发，支持在各系统平台部署使用。

## 功能介绍

1.  团队管理：管理团队中的项目、成员。
2.  项目管理：管理项目中的缺陷、成员。
3.  用例管理：管理测试用例
4.  缺陷管理：管理BUG、需求、任务。
5.  模块管理：维护项目中的软件模块。
6.  API管理：用于管理API接口密钥

## 最新版本更新说明

当前最新版本是0.2.0

* 添加了OPEN API功能，用于第三方系统与Cat2Bug-Platform交互；
* 添加H2数据库部署，使系统直接执行一个Jar文件即可使用，已达到真正轻量、快速使用的目的；

## 我们的特色

* AI自动生成测试用例并录入到系统，解决费时费力录入用例的痛点；
* 丰富便捷的图形化界面操作，减少录入管理成本；
* 专注于软件的缺陷的跟踪管理，简单直接，即开即用，减少学习成本；

## 在线体验

- 体验账号：demo
- 体验密码：123456  

演示地址：[http://www.cat2bug.com:8022](http://www.cat2bug.com:8022)

## 部署

目前我们提供了一套Docker容器化的部署方案，执行命令如下：

```
curl -o docker-compose.yml http://cat2bug.com/docker/docker-compose.yml
docker-compose up -d
```

系统管理员账号：admin    密码：cat2bug

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

目前Cat2Bug还在持续成长中，后续我们将在测试工具、自动化、AI几个方向持续投入，完善平台的功能。2024计划如下：

* cat2bug-platform: 功能叠加，完善系统统计管理功能；
* cat2bug-app：提供移动端APP；
* cat2bug-spring-junit：提供Java Spring自动化单元测试；
* cat2bug-cloud：cat2bug云平台的建设；

## Cat2Bug交流群

QQ群： [731462000](https://qm.qq.com/cgi-bin/qm/qr?k=G_vJa478flcFo_1ohJxNYD0mRKafQ7I1&jump_from=webapi&authKey=EL0KrLpnjYWqNN9YXTVksNlNFrV9DHYyPMx2RVOhXqLzfnmc+Oz8oQ38aBOGx90t)
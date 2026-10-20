# 对话框单输入框回车触发表单原生 submit 整页刷新

- 日期：2026-10-20
- 涉及：后台（Vue / Element Plus）

## 现象

文档「新建文件夹」对话框只有一个名称输入框时，在框内按回车，页面像刷新了一下，文件夹没有创建成功。

## 根因

`el-form` 会渲染成原生 `<form>`。HTML 约定：表单内**只有一个**文本输入时，回车会触发原生 `submit`。若未拦截，浏览器按默认导航处理，业务 `submitForm` 不会跑（或请求被打断）。

## 解决办法

对话框表单加 `@submit.prevent`（或 `@submit.prevent="submitForm"`）；确定/取消按钮加 `native-type="button"`，避免再当 submit 钮。需要回车即保存时，由 prevent 后的处理函数走业务提交。

## 以后怎么避免

新建/编辑类对话框凡带 `el-form`，默认写上 `@submit.prevent`；单字段表单尤须注意。见 [[组件与交互约定]]。

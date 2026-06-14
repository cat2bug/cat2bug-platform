-- 生产包移除 dev 工具：隐藏表单构建、代码生成及空目录「系统工具」
UPDATE sys_menu SET visible = '1' WHERE menu_id IN (3, 115, 116);

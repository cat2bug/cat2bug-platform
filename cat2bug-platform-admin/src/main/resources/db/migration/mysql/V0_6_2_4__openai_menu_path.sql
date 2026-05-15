-- OpenAI 账号前端路由由 /project/system/project/ai/account 调整为 /project/openai（sys_menu.path 与父级 project 拼接）
UPDATE sys_menu SET path = 'openai' WHERE menu_id = 2126;

-- Ollama/AI 模型配置前端路由由 /project/project-ai 调整为 /project/ollama（sys_menu.path 与父级 project 拼接）
UPDATE sys_menu SET path = 'ollama' WHERE menu_id = 2100;

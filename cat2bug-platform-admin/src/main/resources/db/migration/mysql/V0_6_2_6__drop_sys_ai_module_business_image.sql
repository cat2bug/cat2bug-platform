-- 移除项目 Ollama 配置表中已不再使用的「业务识别模型」「图像处理模型」列；模型选择在用例/缺陷侧完成
ALTER TABLE sys_ai_module_config DROP COLUMN business_module;
ALTER TABLE sys_ai_module_config DROP COLUMN image_module;

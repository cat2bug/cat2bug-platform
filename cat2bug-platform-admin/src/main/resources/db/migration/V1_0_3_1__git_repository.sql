-- 项目 Git 代码仓库元数据 + 菜单权限

CREATE TABLE IF NOT EXISTS sys_git_repository (
  repository_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '仓库ID',
  project_id BIGINT NOT NULL COMMENT '项目ID',
  name VARCHAR(100) NOT NULL COMMENT '显示名称',
  slug VARCHAR(100) NOT NULL COMMENT 'URL slug（项目内唯一）',
  disk_path VARCHAR(500) NOT NULL COMMENT '磁盘绝对路径（bare 仓）',
  default_branch VARCHAR(100) DEFAULT 'main' COMMENT '默认分支名（空仓可由首次 push 决定）',
  create_by VARCHAR(64) COMMENT '创建者',
  create_time DATETIME COMMENT '创建时间',
  update_by VARCHAR(64) COMMENT '更新者',
  update_time DATETIME COMMENT '更新时间',
  remark VARCHAR(500) COMMENT '备注'
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_git_repository_project_slug ON sys_git_repository (project_id, slug);
CREATE INDEX IF NOT EXISTS idx_sys_git_repository_project_id ON sys_git_repository (project_id);

-- 菜单：项目功能(2013) 下「源码管理」
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2150, '源码管理', 2013, 12, 'git-repo', 'system/git/repo/index', NULL, 1, 0, 'C', '0', '0', 'system:git:repo:list', 'code', 'admin', CURRENT_TIMESTAMP, '', NULL, '项目源码管理', 'git.repo.menu'
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2150);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2151, '仓库管理', 2150, 1, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:git:repo:manage', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2151);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2152, '仓库推送', 2150, 2, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:git:repo:push', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2152);

INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `menu_name_i18n_key`)
SELECT 2153, '仓库拉取', 2150, 3, '#', '', NULL, 1, 0, 'F', '0', '0', 'system:git:repo:pull', '#', 'admin', CURRENT_TIMESTAMP, '', NULL, '', NULL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `menu_id` = 2153);

-- manage: project.admin(6), project.create-by(11)
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 6, 2150 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 6 AND `menu_id` = 2150);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 6, 2151 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 6 AND `menu_id` = 2151);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 6, 2152 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 6 AND `menu_id` = 2152);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 6, 2153 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 6 AND `menu_id` = 2153);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 11, 2150 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 11 AND `menu_id` = 2150);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 11, 2151 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 11 AND `menu_id` = 2151);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 11, 2152 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 11 AND `menu_id` = 2152);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 11, 2153 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 11 AND `menu_id` = 2153);

-- pull+push: project.develop(7), project.tester(8) — 无 manage
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 7, 2150 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 7 AND `menu_id` = 2150);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 7, 2152 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 7 AND `menu_id` = 2152);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 7, 2153 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 7 AND `menu_id` = 2153);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 8, 2150 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 8 AND `menu_id` = 2150);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 8, 2152 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 8 AND `menu_id` = 2152);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) SELECT 8, 2153 WHERE NOT EXISTS (SELECT 1 FROM `sys_role_menu` WHERE `role_id` = 8 AND `menu_id` = 2153);

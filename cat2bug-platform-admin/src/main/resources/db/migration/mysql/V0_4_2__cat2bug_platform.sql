SET FOREIGN_KEY_CHECKS=0;

ALTER TABLE `sys_comment` MODIFY COLUMN `comment_content` varchar(1024) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '评论内容' AFTER `comment_id`;

UPDATE `sys_user` SET `dept_id` = 00000000000000000000, `user_name` = 'admin', `nick_name` = '黑猫警长', `user_type` = '00', `email` = 'admin@cat2bug.com', `phonenumber` = '18888888888', `sex` = '0', `avatar` = '', `password` = '$2a$10$/YbsRyezA9pg13iJhCNE.u5yOvWbuq7NZhOlliUvycEfBIgJN6qHK', `status` = '0', `del_flag` = '0', `login_ip` = '127.0.0.1', `login_date` = '2024-07-30 00:53:15', `create_by` = 'admin', `create_time` = '2023-11-12 15:34:51', `update_by` = '', `update_time` = '2024-07-29 16:53:15', `remark` = '管理员', `ding_user_id` = NULL, `wechat_user_id` = NULL WHERE `user_id` = 1;

SET FOREIGN_KEY_CHECKS=1;


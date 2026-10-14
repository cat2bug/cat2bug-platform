-- Cat2Bug 1.1.0 迁移 #10: git_repo_member_invite
-- 仓级成员邀请：sys_member_invite.repository_id

ALTER TABLE sys_member_invite ADD COLUMN repository_id BIGINT NULL AFTER project_id;
CREATE INDEX idx_sys_member_invite_repository ON sys_member_invite (repository_id);

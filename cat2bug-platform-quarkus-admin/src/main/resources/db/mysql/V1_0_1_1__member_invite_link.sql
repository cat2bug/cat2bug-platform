CREATE TABLE sys_member_invite (
    invite_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(64) NOT NULL,
    scope VARCHAR(16) NOT NULL,
    team_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    role_ids VARCHAR(512) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked TINYINT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    UNIQUE KEY uk_sys_member_invite_token (token),
    KEY idx_sys_member_invite_team (team_id),
    KEY idx_sys_member_invite_project (project_id)
);

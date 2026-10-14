package com.cat2bug.admin.mcp;

import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.system.service.mcp.McpProjectAccess;

/**
 * 一次工具调用的上下文：调用人（令牌对应的用户）、客户端名、来源 IP，以及目标项目内的有效权限。
 */
public record McpScope(LoginUser caller, String clientName, String clientIp, McpProjectAccess access) {

    public Long userId() {
        return caller.getUserId();
    }

    public Long projectId() {
        return access.projectId();
    }
}

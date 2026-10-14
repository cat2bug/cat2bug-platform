package com.cat2bug.common.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;

/**
 * 登录会话（Redis 存储形态与 Spring {@code LoginUser} 字段子集对齐）。
 */
public class LoginUser {

    private Long userId;
    private Long deptId;
    private String token;
    private Long loginTime;
    private Long expireTime;
    private String ipaddr;
    private String loginLocation;
    private String browser;
    private String os;
    private Set<String> permissions;
    private SysUser user;
    /** 是否已按当前团队/项目上下文合并过角色权限（旧会话兼容） */
    private boolean contextPermissionsMerged;
    /** MCP 内部会话：来源渠道（{@code mcp}）与客户端名称，用于审计；网页会话为空 */
    private String operChannel;
    private String clientName;
    /** MCP 内部会话绑定的项目，权限按该项目计算 */
    private Long mcpProjectId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(Long loginTime) {
        this.loginTime = loginTime;
    }

    public Long getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    public String getIpaddr() {
        return ipaddr;
    }

    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    public String getLoginLocation() {
        return loginLocation;
    }

    public void setLoginLocation(String loginLocation) {
        this.loginLocation = loginLocation;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public SysUser getUser() {
        return user;
    }

    public void setUser(SysUser user) {
        this.user = user;
    }

    public boolean isContextPermissionsMerged() {
        return contextPermissionsMerged;
    }

    public void setContextPermissionsMerged(boolean contextPermissionsMerged) {
        this.contextPermissionsMerged = contextPermissionsMerged;
    }

    @JsonIgnore
    public String getUsername() {
        return user != null ? user.getUserName() : null;
    }

    public String getOperChannel() {
        return operChannel;
    }

    public void setOperChannel(String operChannel) {
        this.operChannel = operChannel;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Long getMcpProjectId() {
        return mcpProjectId;
    }

    public void setMcpProjectId(Long mcpProjectId) {
        this.mcpProjectId = mcpProjectId;
    }

    @JsonIgnore
    public boolean isMcpSession() {
        return "mcp".equals(operChannel);
    }
}

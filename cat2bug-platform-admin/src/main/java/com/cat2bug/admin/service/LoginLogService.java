package com.cat2bug.admin.service;

import com.cat2bug.common.domain.SysLogininfor;
import com.cat2bug.framework.web.ClientInfo;
import com.cat2bug.system.mapper.SysUserMapper;
import com.cat2bug.system.service.SysLogininforService;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * 登录日志（sys_logininfor）与用户最近登录信息。
 */
@ApplicationScoped
public class LoginLogService {

    private static final Logger LOG = Logger.getLogger(LoginLogService.class);

    public static final String SUCCESS = "0";
    public static final String FAIL = "1";

    @Inject
    SysLogininforService sysLogininforService;

    @Inject
    SysUserMapper sysUserMapper;

    /** 须在请求线程调用：客户端信息取自当前请求，写库异步执行。 */
    public void record(String username, String status, String message) {
        ClientInfo client = ClientInfo.current();
        Infrastructure.getDefaultWorkerPool().execute(() -> {
            try {
                SysLogininfor info = new SysLogininfor();
                info.setUserName(username);
                info.setIpaddr(client.ip());
                info.setLoginLocation(ClientInfo.resolveLocation(client.ip()));
                info.setBrowser(client.browser());
                info.setOs(client.os());
                info.setMsg(message);
                info.setStatus(status);
                sysLogininforService.insertLogininfor(info);
            } catch (RuntimeException e) {
                LOG.warnf("记录登录日志失败 user=%s: %s", username, e.getMessage());
            }
        });
    }

    public void recordLoginInfo(Long userId) {
        sysUserMapper.updateLoginInfo(userId, ClientInfo.current().ip());
    }

    public static boolean isMatchedIp(String filter, String ip) {
        if (filter == null || filter.isBlank() || ip == null || ip.isBlank()) {
            return false;
        }
        for (String rule : filter.split(";")) {
            String r = rule.trim();
            if (r.isEmpty()) {
                continue;
            }
            if (r.contains("-")) {
                String[] range = r.split("-", 2);
                long from = ipToLong(range[0].trim());
                long to = ipToLong(range[1].trim());
                long value = ipToLong(ip);
                if (from >= 0 && to >= 0 && value >= 0 && value >= Math.min(from, to) && value <= Math.max(from, to)) {
                    return true;
                }
            } else if (r.contains("*")) {
                String[] ruleParts = r.split("\\.");
                String[] ipParts = ip.split("\\.");
                if (ruleParts.length == 4 && ipParts.length == 4) {
                    boolean matched = true;
                    for (int i = 0; i < 4 && matched; i++) {
                        matched = "*".equals(ruleParts[i]) || ruleParts[i].equals(ipParts[i]);
                    }
                    if (matched) {
                        return true;
                    }
                }
            } else if (r.equals(ip)) {
                return true;
            }
        }
        return false;
    }

    private static long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return -1;
        }
        long value = 0;
        try {
            for (String part : parts) {
                int n = Integer.parseInt(part);
                if (n < 0 || n > 255) {
                    return -1;
                }
                value = (value << 8) | n;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
        return value;
    }
}

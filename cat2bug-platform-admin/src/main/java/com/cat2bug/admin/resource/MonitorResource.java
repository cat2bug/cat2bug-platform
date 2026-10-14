package com.cat2bug.admin.resource;

import com.cat2bug.admin.service.CacheMonitorService;
import com.cat2bug.admin.service.ServerInfoService;
import com.cat2bug.common.api.AjaxResult;
import com.cat2bug.common.api.TableDataInfo;
import com.cat2bug.common.domain.LoginUser;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.framework.security.RequiresPermissions;
import com.cat2bug.framework.security.TokenService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统监控：在线用户、服务器、缓存。
 */
@Path("/monitor")
@Produces(MediaType.APPLICATION_JSON)
public class MonitorResource {

    @Inject
    TokenService tokenService;

    @Inject
    ServerInfoService serverInfoService;

    @Inject
    CacheMonitorService cacheMonitorService;

    @GET
    @Path("/online/list")
    @RequiresPermissions("monitor:online:list")
    public TableDataInfo onlineList(@QueryParam("ipaddr") String ipaddr, @QueryParam("userName") String userName) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LoginUser user : tokenService.listOnlineUsers()) {
            if (notBlank(ipaddr) && !ipaddr.equals(user.getIpaddr())) {
                continue;
            }
            if (notBlank(userName) && (user.getUser() == null || !userName.equals(user.getUser().getUserName()))) {
                continue;
            }
            rows.add(toOnline(user));
        }
        Collections.reverse(rows);
        return new TableDataInfo(rows, rows.size());
    }

    @DELETE
    @Path("/online/{tokenId}")
    @RequiresPermissions("monitor:online:forceLogout")
    public AjaxResult forceLogout(@PathParam("tokenId") String tokenId) {
        tokenService.delLoginUser(tokenId);
        return AjaxResult.success();
    }

    @GET
    @Path("/server")
    @RequiresPermissions("monitor:server:list")
    public AjaxResult server() {
        return AjaxResult.success(serverInfoService.collect());
    }

    @GET
    @Path("/cache")
    @RequiresPermissions("monitor:cache:list")
    public AjaxResult cacheInfo() {
        return AjaxResult.success(cacheMonitorService.info());
    }

    @GET
    @Path("/cache/getNames")
    @RequiresPermissions("monitor:cache:list")
    public AjaxResult cacheNames() {
        return AjaxResult.success(cacheMonitorService.cacheNames());
    }

    @GET
    @Path("/cache/getKeys/{cacheName}")
    @RequiresPermissions("monitor:cache:list")
    public AjaxResult cacheKeys(@PathParam("cacheName") String cacheName) {
        return AjaxResult.success(cacheMonitorService.keys(cacheName));
    }

    @GET
    @Path("/cache/getValue/{cacheName}/{cacheKey}")
    @RequiresPermissions("monitor:cache:list")
    public AjaxResult cacheValue(@PathParam("cacheName") String cacheName, @PathParam("cacheKey") String cacheKey) {
        return AjaxResult.success(cacheMonitorService.value(cacheName, cacheKey));
    }

    @DELETE
    @Path("/cache/clearCacheName/{cacheName}")
    @RequiresPermissions("monitor:cache:list")
    public AjaxResult clearCacheName(@PathParam("cacheName") String cacheName) {
        cacheMonitorService.clearCacheName(cacheName);
        return AjaxResult.success();
    }

    @DELETE
    @Path("/cache/clearCacheKey/{cacheKey}")
    @RequiresPermissions("monitor:cache:list")
    public AjaxResult clearCacheKey(@PathParam("cacheKey") String cacheKey) {
        cacheMonitorService.clearCacheKey(cacheKey);
        return AjaxResult.success();
    }

    @DELETE
    @Path("/cache/clearCacheAll")
    @RequiresPermissions("monitor:cache:list")
    public AjaxResult clearCacheAll() {
        cacheMonitorService.clearAll();
        return AjaxResult.success();
    }

    private static Map<String, Object> toOnline(LoginUser user) {
        SysUser sysUser = user.getUser();
        Map<String, Object> online = new LinkedHashMap<>();
        online.put("tokenId", user.getToken());
        online.put("userName", sysUser != null ? sysUser.getUserName() : null);
        online.put("deptName", sysUser != null && sysUser.getDept() != null ? sysUser.getDept().getDeptName() : null);
        online.put("ipaddr", user.getIpaddr());
        online.put("loginLocation", user.getLoginLocation());
        online.put("browser", user.getBrowser());
        online.put("os", user.getOs());
        online.put("loginTime", user.getLoginTime());
        return online;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isEmpty();
    }
}

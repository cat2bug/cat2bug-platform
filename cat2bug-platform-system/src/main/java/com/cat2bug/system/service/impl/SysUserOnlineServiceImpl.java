package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.WebSocketResult;
import com.cat2bug.system.websocket.MessageWebsocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.cat2bug.common.core.domain.model.LoginUser;
import com.cat2bug.common.utils.StringUtils;
import com.cat2bug.system.domain.SysUserOnline;
import com.cat2bug.system.service.ISysUserOnlineService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 在线用户 服务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysUserOnlineServiceImpl implements ISysUserOnlineService
{
    private final static String MEMBER_ONLINE = "memberOnline";
    private final static String MEMBER_OFFLINE = "memberOffline";

    private static Map<Long, AtomicInteger> onlineMap = new ConcurrentHashMap<>();

    @Autowired
    private MessageWebsocket messageWebsocket;

    /**
     * 通过登录地址查询信息
     * 
     * @param ipaddr 登录地址
     * @param user 用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByIpaddr(String ipaddr, LoginUser user)
    {
        if (StringUtils.equals(ipaddr, user.getIpaddr()))
        {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过用户名称查询信息
     * 
     * @param userName 用户名称
     * @param user 用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByUserName(String userName, LoginUser user)
    {
        if (StringUtils.equals(userName, user.getUsername()))
        {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 通过登录地址/用户名称查询信息
     * 
     * @param ipaddr 登录地址
     * @param userName 用户名称
     * @param user 用户信息
     * @return 在线用户信息
     */
    @Override
    public SysUserOnline selectOnlineByInfo(String ipaddr, String userName, LoginUser user)
    {
        if (StringUtils.equals(ipaddr, user.getIpaddr()) && StringUtils.equals(userName, user.getUsername()))
        {
            return loginUserToUserOnline(user);
        }
        return null;
    }

    /**
     * 设置在线用户信息
     * 
     * @param user 用户信息
     * @return 在线用户
     */
    @Override
    public SysUserOnline loginUserToUserOnline(LoginUser user)
    {
        if (StringUtils.isNull(user) || StringUtils.isNull(user.getUser()))
        {
            return null;
        }
        SysUserOnline sysUserOnline = new SysUserOnline();
        sysUserOnline.setTokenId(user.getToken());
        sysUserOnline.setUserName(user.getUsername());
        sysUserOnline.setIpaddr(user.getIpaddr());
        sysUserOnline.setLoginLocation(user.getLoginLocation());
        sysUserOnline.setBrowser(user.getBrowser());
        sysUserOnline.setOs(user.getOs());
        sysUserOnline.setLoginTime(user.getLoginTime());
        if (StringUtils.isNotNull(user.getUser().getDept()))
        {
            sysUserOnline.setDeptName(user.getUser().getDept().getDeptName());
        }
        return sysUserOnline;
    }

    @Override
    public boolean isOnline(Long memberId) {
        return onlineMap.containsKey(memberId);
    }

    @Override
    public void memberOnline(Long memberId) {
        // 设置成员在线
        if(onlineMap.containsKey(memberId)==false) {
            onlineMap.put(memberId, new AtomicInteger());
        }
        onlineMap.get(memberId).incrementAndGet();
        // 发送成员在线实时消息
        WebSocketResult result = WebSocketResult.success(MEMBER_ONLINE,memberId);
        messageWebsocket.sendMessage(result);
    }

    @Override
    public void memberOffline(Long memberId) {
        // 设置成员离线
        if(onlineMap.containsKey(memberId)) {
            int count = onlineMap.get(memberId).decrementAndGet();
            // 如果所有链接都断开了，发送成员离线信息
            if(count<=0) {
                onlineMap.remove(memberId);
                // 发送成员离线实时消息
                WebSocketResult result = WebSocketResult.success(MEMBER_OFFLINE,memberId);
                messageWebsocket.sendMessage(result);
            }
        }
    }
}

package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.WebSocketResult;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.redis.RedisCache;
import com.cat2bug.common.websocket.MessageWebsocket;
import com.cat2bug.system.domain.MemberFocus;
import com.cat2bug.system.service.IMemberFocusService;
import com.cat2bug.system.websocket.MemberFocusWebsocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: yuzhantao
 * @CreateTime: 2024-03-22 00:00
 * @Version: 1.0.0
 */
@Service
public class MemberFocusServiceImpl implements IMemberFocusService {
    private final static String MEMBER_FOCUS = "memberFocus";

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private MessageWebsocket messageWebsocket;

    @Override
    public void setFocus(String moduleName, Long dataId, SysUser user) {
        MemberFocus mf = new MemberFocus(moduleName,dataId,user);
        this.redisCache.setCacheObject(
                MEMBER_FOCUS,
                String.format("%s-%d-%d",moduleName, dataId, user.getUserId()),
                mf);
        Collection<String> keys = this.redisCache.getKeys(MEMBER_FOCUS);
        messageWebsocket.sendMessage(WebSocketResult.success(MEMBER_FOCUS,mf));
    }

    @Override
    public void removeFocus(Long memberId) {
        SysUser user = new SysUser();
        user.setUserId(memberId);
        MemberFocus mf = new MemberFocus(null,null,user);
        Collection<String> keys = this.redisCache.getKeys(MEMBER_FOCUS);
        keys.forEach(k->{
            if(k.endsWith(String.format("-%d",memberId))){
                this.redisCache.deleteObject(MEMBER_FOCUS,k);
                messageWebsocket.sendMessage(WebSocketResult.success(MEMBER_FOCUS,mf));
            }
        });
    }

    @Override
    public List<SysUser> getFocusMemberList(String moduleName, Long dataId) {
        Collection<String> keys = this.redisCache.getKeys(MEMBER_FOCUS);
        return keys.stream().filter(k -> k.indexOf(String.format("%s-%d-",moduleName,dataId))==0).map(k->{
            MemberFocus mf = redisCache.getCacheObject(MEMBER_FOCUS,k);
            if(mf==null) return null;
            return mf.getUser();
        }).filter(k -> k!=null).collect(Collectors.toList());
    }
}

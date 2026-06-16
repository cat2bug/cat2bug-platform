package com.cat2bug.system.service.impl;

import com.cat2bug.common.core.domain.WebSocketResult;
import com.cat2bug.common.core.domain.entity.SysUser;
import com.cat2bug.common.core.redis.RedisCache;
import com.cat2bug.common.websocket.MessageWebsocket;
import com.cat2bug.system.domain.MemberFocus;
import com.cat2bug.system.service.IMemberFocusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        if (dataId == null) {
            return Collections.emptyList();
        }
        Map<Long, List<SysUser>> map = getFocusMemberMap(moduleName, Collections.singleton(dataId));
        return map.getOrDefault(dataId, Collections.emptyList());
    }

    @Override
    public Map<Long, List<SysUser>> getFocusMemberMap(String moduleName, Collection<Long> dataIds) {
        if (moduleName == null || dataIds == null || dataIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> wanted = dataIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));
        if (wanted.isEmpty()) {
            return Collections.emptyMap();
        }
        String modulePrefix = moduleName + "-";
        Map<Long, List<SysUser>> result = new HashMap<>();
        for (String key : this.redisCache.getKeys(MEMBER_FOCUS)) {
            if (!key.startsWith(modulePrefix)) {
                continue;
            }
            Long dataId = parseFocusDataId(moduleName, key);
            if (dataId == null || !wanted.contains(dataId)) {
                continue;
            }
            try {
                MemberFocus mf = redisCache.getCacheObject(MEMBER_FOCUS, key);
                if (mf != null && mf.getUser() != null) {
                    result.computeIfAbsent(dataId, ignored -> new ArrayList<>()).add(mf.getUser());
                }
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    private static Long parseFocusDataId(String moduleName, String key) {
        String prefix = moduleName + "-";
        if (!key.startsWith(prefix)) {
            return null;
        }
        String rest = key.substring(prefix.length());
        int dash = rest.indexOf('-');
        if (dash <= 0) {
            return null;
        }
        try {
            return Long.parseLong(rest.substring(0, dash));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

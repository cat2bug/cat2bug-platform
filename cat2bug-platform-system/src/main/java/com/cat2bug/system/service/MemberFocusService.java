package com.cat2bug.system.service;

import com.cat2bug.common.api.WebSocketResult;
import com.cat2bug.common.domain.MemberFocus;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.websocket.MessageWebsocketPush;
import com.cat2bug.framework.cache.AppCache;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 成员编辑焦点服务，与 Spring {@code MemberFocusServiceImpl} 行为对齐。
 */
@ApplicationScoped
public class MemberFocusService {

    static final String MEMBER_FOCUS = "memberFocus";

    @Inject
    AppCache appCache;

    @Inject
    MessageWebsocketPush messageWebsocketPush;

    public void setFocus(String moduleName, Long dataId, SysUser user) {
        MemberFocus mf = new MemberFocus(moduleName, dataId, user);
        appCache.set(MEMBER_FOCUS, focusKey(moduleName, dataId, user.getUserId()), mf);
        messageWebsocketPush.sendToAll(WebSocketResult.success(MEMBER_FOCUS, mf));
    }

    public void removeFocus(Long memberId) {
        SysUser user = new SysUser();
        user.setUserId(memberId);
        MemberFocus mf = new MemberFocus(null, null, user);
        Collection<String> keys = appCache.keysInRegion(MEMBER_FOCUS);
        keys.forEach(k -> {
            if (k.endsWith("-" + memberId)) {
                appCache.remove(MEMBER_FOCUS, k);
                messageWebsocketPush.sendToAll(WebSocketResult.success(MEMBER_FOCUS, mf));
            }
        });
    }

    public List<SysUser> getFocusMemberList(String moduleName, Long dataId) {
        String prefix = moduleName + "-" + dataId + "-";
        Collection<String> keys = appCache.keysInRegion(MEMBER_FOCUS);
        return keys.stream()
                .filter(k -> k.startsWith(prefix))
                .map(k -> {
                    try {
                        MemberFocus mf = appCache.get(MEMBER_FOCUS, k, MemberFocus.class);
                        if (mf == null) {
                            return null;
                        }
                        return mf.getUser();
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(u -> u != null)
                .collect(Collectors.toList());
    }

    private static String focusKey(String moduleName, Long dataId, Long userId) {
        return String.format("%s-%d-%d", moduleName, dataId, userId);
    }
}

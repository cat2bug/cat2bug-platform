package com.cat2bug.admin.service;

import com.cat2bug.admin.websocket.ImWebSocketNotifier;
import com.cat2bug.common.domain.SysNotice;
import com.cat2bug.common.domain.im.IMSystemPlatformConfig;
import com.cat2bug.common.domain.im.IMUserConfig;
import com.cat2bug.common.domain.vo.SendNotice;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.ImNoticeDefaults;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.service.ImUserConfigService;
import com.cat2bug.system.service.SysNoticeService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ImNoticeDispatchService {

    @Inject
    ImUserConfigService imUserConfigService;

    @Inject
    SysNoticeService sysNoticeService;

    @Inject
    ImWebSocketNotifier imWebSocketNotifier;

    public void sendNotice(Long senderId, SendNotice request) {
        if (request == null || request.getProjectId() == null) {
            throw new ApiException("项目ID不能为空");
        }
        if (StringUtils.isBlank(request.getTitle())) {
            throw new ApiException("通知标题不能为空");
        }
        List<Long> receiveIds = request.getReceiveIds();
        if (receiveIds == null || receiveIds.isEmpty()) {
            throw new ApiException("接收人不能为空");
        }
        String sn = UUID.randomUUID().toString().replace("-", "");
        String content = request.getContent() != null ? request.getContent() : "";
        String src = request.getSrc() != null ? request.getSrc() : "";
        String group = request.getGroupName() != null ? request.getGroupName() : "default";

        for (Long recipientId : receiveIds) {
            if (recipientId == null) {
                continue;
            }
            IMUserConfig userConfig = imUserConfigService.selectImUserConfigByProjectAndMember(
                    request.getProjectId(), recipientId, ImNoticeDefaults.defaultDefectNoticeOption());
            IMSystemPlatformConfig system = userConfig.getConfig() != null
                    && userConfig.getConfig().getPlatforms() != null
                    ? userConfig.getConfig().getPlatforms().getSystem() : null;
            if (system == null || !system.isConfigSwitch()) {
                continue;
            }
            SysNotice notice = new SysNotice();
            notice.setNoticeId(sn);
            notice.setNoticeTitle(trimTitle(request.getTitle()));
            notice.setNoticeType("1");
            notice.setNoticeContent(content);
            notice.setStatus("0");
            notice.setProjectId(request.getProjectId());
            notice.setGroupName(group);
            notice.setReceiverId(recipientId);
            if (sysNoticeService.insertReceiverNotice(notice) <= 0) {
                throw new ApiException("保存通知失败");
            }
            Map<String, Object> payload = buildPayload(notice, system, src, senderId);
            imWebSocketNotifier.pushNotice(recipientId, payload);
            if (system.isPanel()) {
                imWebSocketNotifier.pushPanelNotice(recipientId, payload);
            }
        }
    }

    private static String trimTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.length() > 255 ? title.substring(0, 255) : title;
    }

    private static Map<String, Object> buildPayload(SysNotice notice, IMSystemPlatformConfig system,
                                                    String src, Long senderId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("noticeId", notice.getNoticeId());
        payload.put("title", notice.getNoticeTitle());
        payload.put("content", notice.getNoticeContent());
        payload.put("noticeType", notice.getNoticeType());
        payload.put("status", notice.getStatus());
        payload.put("projectId", notice.getProjectId());
        payload.put("group", notice.getGroupName());
        payload.put("receiveMemberId", notice.getReceiverId());
        payload.put("src", src);
        payload.put("senderId", senderId);
        payload.put("backgroundMusic", system.isBackgroundMusic());
        payload.put("backgroundMusicUrl", system.getBackgroundMusicUrl());
        payload.put("panel", system.isPanel());
        payload.put("type", "MARKDOWN");
        return payload;
    }
}

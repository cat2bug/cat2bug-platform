package com.cat2bug.admin.service;

import com.cat2bug.admin.service.im.ImEvent;
import com.cat2bug.admin.service.im.ImLinkBuilder;
import com.cat2bug.admin.service.im.ImMessageTemplates;
import com.cat2bug.admin.websocket.ImWebSocketNotifier;
import com.cat2bug.common.domain.SysNotice;
import com.cat2bug.common.domain.SysUser;
import com.cat2bug.common.domain.im.DingProjectConfig;
import com.cat2bug.common.domain.im.EnterpriseWeChatPlatformConfig;
import com.cat2bug.common.domain.im.EnterpriseWeChatProjectConfig;
import com.cat2bug.common.domain.im.FeishuPlatformConfig;
import com.cat2bug.common.domain.im.FeishuProjectConfig;
import com.cat2bug.common.domain.im.IMConfig;
import com.cat2bug.common.domain.im.IMDingPlatformConfig;
import com.cat2bug.common.domain.im.IMMailPlatformConfig;
import com.cat2bug.common.domain.im.IMPlatformConfig;
import com.cat2bug.common.domain.im.IMSystemPlatformConfig;
import com.cat2bug.common.domain.im.IMUserConfig;
import com.cat2bug.common.domain.vo.SendNotice;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.util.ImNoticeDefaults;
import com.cat2bug.common.util.StringUtils;
import com.cat2bug.system.config.ImPushAsyncConfig;
import com.cat2bug.system.service.ImProjectConfigService;
import com.cat2bug.system.service.ImUserConfigService;
import com.cat2bug.system.service.SysNoticeService;
import com.cat2bug.system.service.SysUserService;
import com.cat2bug.system.service.im.DingClient;
import com.cat2bug.system.service.im.FeishuClient;
import com.cat2bug.system.service.im.ImMailSender;
import com.cat2bug.system.service.im.WeComClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * IM 通知分发入口（对齐 Spring {@code IMService#sendMessage}）：站内通知在请求线程内同步写入，
 * 邮件、钉钉、飞书、企业微信在 {@code imPushTaskExecutor} 中逐个「渠道 × 接收人」异步发送，互不影响。
 * <p>
 * 群机器人每个事件只发一条，取第一个接收人的配置。
 */
@ApplicationScoped
public class ImNoticeDispatchService {

    private static final Logger LOG = Logger.getLogger(ImNoticeDispatchService.class);
    static final String DETAIL_BUTTON_TITLE = "点击查看详情";

    @Inject
    ImUserConfigService imUserConfigService;

    @Inject
    SysNoticeService sysNoticeService;

    @Inject
    ImWebSocketNotifier imWebSocketNotifier;

    @Inject
    ImProjectConfigService imProjectConfigService;

    @Inject
    SysUserService sysUserService;

    @Inject
    ImLinkBuilder imLinkBuilder;

    @Inject
    ImMailSender imMailSender;

    @Inject
    DingClient dingClient;

    @Inject
    FeishuClient feishuClient;

    @Inject
    WeComClient weComClient;

    @Inject
    @Named(ImPushAsyncConfig.EXECUTOR_BEAN_NAME)
    ExecutorService imPushTaskExecutor;

    private record Recipient(Long memberId, IMConfig config) {
    }

    /** 前端「发送通知」：校验后按默认（通知）模版分发。 */
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
        String group = request.getGroupName() != null ? request.getGroupName() : "default";
        dispatch(new ImEvent(request.getProjectId(), group, senderId, receiveIds, request.getTitle(),
                imLinkBuilder.notice(request.getSrc(), request.getProjectId()),
                ImMessageTemplates.notice(request.getTitle(), request.getContent()),
                null, ImNoticeDefaults.defaultDefectNoticeOption()));
    }

    public void dispatch(ImEvent event) {
        if (event == null || event.projectId() == null || event.receiverIds() == null || event.receiverIds().isEmpty()) {
            return;
        }
        String sn = UUID.randomUUID().toString().replace("-", "");
        List<Recipient> recipients = loadRecipients(event);
        if (recipients.isEmpty()) {
            return;
        }
        for (Recipient recipient : recipients) {
            sendSiteNotice(sn, event, recipient);
        }
        submitExternal(sn, event, recipients);
    }

    private List<Recipient> loadRecipients(ImEvent event) {
        Map<Long, Recipient> recipients = new LinkedHashMap<>();
        for (Long memberId : event.receiverIds()) {
            if (memberId == null || recipients.containsKey(memberId)) {
                continue;
            }
            try {
                IMUserConfig userConfig = imUserConfigService.selectImUserConfigByProjectAndMember(
                        event.projectId(), memberId, event.defaultOption());
                IMConfig config = userConfig != null ? userConfig.getConfig() : null;
                if (config == null || config.getPlatforms() == null) {
                    continue;
                }
                if (!realtimeEnabled(config, event.moduleKey())) {
                    continue;
                }
                recipients.put(memberId, new Recipient(memberId, config));
            } catch (RuntimeException e) {
                LOG.warnf("读取 IM 个人配置失败，跳过该接收人 projectId=%d memberId=%d: %s",
                        event.projectId(), memberId, e.getMessage());
            }
        }
        return new ArrayList<>(recipients.values());
    }

    @SuppressWarnings("unchecked")
    static boolean realtimeEnabled(IMConfig config, String moduleKey) {
        if (moduleKey == null) {
            return true;
        }
        Map<String, Object> modules = config.getModules();
        Object module = modules != null ? modules.get(moduleKey) : null;
        if (!(module instanceof Map<?, ?> moduleMap)) {
            return false;
        }
        Object option = moduleMap.get("option");
        if (!(option instanceof Map<?, ?> optionMap)) {
            return false;
        }
        Object realtime = optionMap.get("realtime");
        return realtime instanceof Map<?, ?> realtimeMap && Boolean.TRUE.equals(realtimeMap.get("switch"));
    }

    private void sendSiteNotice(String sn, ImEvent event, Recipient recipient) {
        IMSystemPlatformConfig system = recipient.config().getPlatforms().getSystem();
        if (system == null || !system.isConfigSwitch()) {
            return;
        }
        SysNotice notice = new SysNotice();
        notice.setNoticeId(sn);
        notice.setNoticeTitle(trimTitle(event.title()));
        notice.setNoticeType("1");
        notice.setNoticeContent(event.body().markdown());
        notice.setStatus("0");
        notice.setProjectId(event.projectId());
        notice.setGroupName(event.group() != null ? event.group() : "default");
        notice.setReceiverId(recipient.memberId());
        if (sysNoticeService.insertReceiverNotice(notice) <= 0) {
            throw new ApiException("保存通知失败");
        }
        Map<String, Object> payload = buildPayload(notice, system, event.src(), event.senderId());
        imWebSocketNotifier.pushNotice(recipient.memberId(), payload);
        if (system.isPanel()) {
            imWebSocketNotifier.pushPanelNotice(recipient.memberId(), payload);
        }
    }

    private void submitExternal(String sn, ImEvent event, List<Recipient> recipients) {
        Supplier<DingProjectConfig> dingApp = memoize(() -> imProjectConfigService.selectAppConfig(
                event.projectId(), DingProjectConfig.SYSTEM_CODE, DingProjectConfig.class));
        Supplier<FeishuProjectConfig> feishuApp = memoize(() -> imProjectConfigService.selectAppConfig(
                event.projectId(), FeishuProjectConfig.SYSTEM_CODE, FeishuProjectConfig.class));
        Supplier<EnterpriseWeChatProjectConfig> weComApp = memoize(() -> imProjectConfigService.selectAppConfig(
                event.projectId(), EnterpriseWeChatProjectConfig.SYSTEM_CODE, EnterpriseWeChatProjectConfig.class));

        submitGroupMessages(sn, event, recipients.get(0).config().getPlatforms());
        for (Recipient recipient : recipients) {
            IMPlatformConfig platforms = recipient.config().getPlatforms();
            submitMail(sn, event, recipient.memberId(), platforms.getMail());

            IMDingPlatformConfig ding = platforms.getDing();
            if (ding != null && Boolean.TRUE.equals(ding.getSingleSwitch()) && StringUtils.isNotBlank(ding.getMobile())) {
                DingProjectConfig app = dingApp.get();
                if (app == null) {
                    skipMissingApp(sn, "ding-single", event, recipient.memberId());
                } else {
                    String mobile = ding.getMobile().trim();
                    submit(sn, "ding-single", event, recipient.memberId(), () -> sendDingSingle(app, mobile, event));
                }
            }

            FeishuPlatformConfig feishu = platforms.getFeishu();
            if (feishu != null && Boolean.TRUE.equals(feishu.getSingleSwitch()) && StringUtils.isNotBlank(feishu.getMobile())) {
                FeishuProjectConfig app = feishuApp.get();
                if (app == null || StringUtils.isBlank(app.getAppId()) || StringUtils.isBlank(app.getAppSecret())) {
                    skipMissingApp(sn, "feishu-single", event, recipient.memberId());
                } else {
                    String mobile = feishu.getMobile().trim();
                    submit(sn, "feishu-single", event, recipient.memberId(),
                            () -> feishuClient.sendUserText(app, mobile, event.body().text()));
                }
            }

            EnterpriseWeChatPlatformConfig weChat = platforms.getWechat();
            if (weChat != null && Boolean.TRUE.equals(weChat.getSingleSwitch()) && StringUtils.isNotBlank(weChat.getMobile())) {
                EnterpriseWeChatProjectConfig app = weComApp.get();
                if (app == null) {
                    skipMissingApp(sn, "wechat-single", event, recipient.memberId());
                } else {
                    String mobile = weChat.getMobile().trim();
                    submit(sn, "wechat-single", event, recipient.memberId(),
                            () -> weComClient.sendAppMessage(app, mobile, "markdown", event.body().markdown()));
                }
            }
        }
    }

    private void submitGroupMessages(String sn, ImEvent event, IMPlatformConfig platforms) {
        IMDingPlatformConfig ding = platforms.getDing();
        if (ding != null && Boolean.TRUE.equals(ding.getGroupSwitch()) && StringUtils.isNotBlank(ding.getHook())) {
            String hook = ding.getHook().trim();
            String secret = ding.getSecret();
            String text = withKeyword(ding.getKey(), event.body().text());
            submit(sn, "ding-group", event, null, () -> dingClient.sendGroupText(hook, secret, text));
        }
        FeishuPlatformConfig feishu = platforms.getFeishu();
        if (feishu != null && Boolean.TRUE.equals(feishu.getGroupSwitch()) && StringUtils.isNotBlank(feishu.getHook())) {
            String hook = feishu.getHook().trim();
            String secret = feishu.getSecret();
            String text = withKeyword(feishu.getKey(), event.body().text());
            submit(sn, "feishu-group", event, null, () -> feishuClient.sendGroupText(hook, secret, text));
        }
        EnterpriseWeChatPlatformConfig weChat = platforms.getWechat();
        if (weChat != null && Boolean.TRUE.equals(weChat.getGroupSwitch()) && StringUtils.isNotBlank(weChat.getHook())) {
            String hook = weChat.getHook().trim();
            String mention = weChat.getMobile();
            submit(sn, "wechat-group", event, null,
                    () -> weComClient.sendGroupMarkdown(hook, event.body().markdown(), mention));
        }
    }

    private void submitMail(String sn, ImEvent event, Long memberId, IMMailPlatformConfig mail) {
        if (mail == null || !mail.isConfigSwitch() || !imMailSender.isConfigured()) {
            return;
        }
        String address = mail.getReceiver();
        if (StringUtils.isBlank(address)) {
            SysUser user = sysUserService.selectUserById(memberId);
            address = user != null ? user.getEmail() : null;
        }
        if (StringUtils.isBlank(address)) {
            return;
        }
        String to = address;
        submit(sn, "mail", event, memberId, () -> imMailSender.send(to, event.title(), event.body().html()));
    }

    private void sendDingSingle(DingProjectConfig app, String mobile, ImEvent event) throws Exception {
        String src = event.src();
        String url = StringUtils.isBlank(src) ? "" : src + (src.contains("?") ? "&" : "?") + "screen=full";
        dingClient.sendRobotActionCard(app, mobile, event.title(), event.body().markdown(), DETAIL_BUTTON_TITLE, url);
    }

    @FunctionalInterface
    interface PushTask {
        void run() throws Exception;
    }

    private void submit(String sn, String channel, ImEvent event, Long memberId, PushTask task) {
        imPushTaskExecutor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                LOG.warnf("IM 推送失败 sn=%s channel=%s projectId=%d memberId=%s: %s",
                        sn, channel, event.projectId(), memberId, e.getMessage());
                LOG.debugf(e, "IM 推送失败详情 sn=%s channel=%s", sn, channel);
            }
        });
    }

    private static void skipMissingApp(String sn, String channel, ImEvent event, Long memberId) {
        LOG.warnf("项目未配置企业应用，跳过 IM 个人消息 sn=%s channel=%s projectId=%d memberId=%d",
                sn, channel, event.projectId(), memberId);
    }

    private static String withKeyword(String key, String text) {
        return StringUtils.isNotBlank(key) ? String.format("【%s】%s", key, text) : text;
    }

    private static <T> Supplier<T> memoize(Supplier<T> loader) {
        return new Supplier<>() {
            private boolean loaded;
            private T value;

            @Override
            public T get() {
                if (!loaded) {
                    value = loader.get();
                    loaded = true;
                }
                return value;
            }
        };
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
        payload.put("src", src != null ? src : "");
        payload.put("senderId", senderId);
        payload.put("backgroundMusic", system.isBackgroundMusic());
        payload.put("backgroundMusicUrl", system.getBackgroundMusicUrl());
        payload.put("panel", system.isPanel());
        payload.put("type", "MARKDOWN");
        return payload;
    }
}

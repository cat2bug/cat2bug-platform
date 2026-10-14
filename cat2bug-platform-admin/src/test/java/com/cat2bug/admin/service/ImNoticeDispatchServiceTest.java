package com.cat2bug.admin.service;

import com.cat2bug.admin.service.im.ImEvent;
import com.cat2bug.admin.service.im.ImLinkBuilder;
import com.cat2bug.admin.service.im.ImMessageBody;
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
import com.cat2bug.common.util.ImNoticeDefaults;
import com.cat2bug.system.service.ImProjectConfigService;
import com.cat2bug.system.service.ImUserConfigService;
import com.cat2bug.system.service.SysNoticeService;
import com.cat2bug.system.service.SysUserService;
import com.cat2bug.system.service.im.DingClient;
import com.cat2bug.system.service.im.FeishuClient;
import com.cat2bug.system.service.im.ImMailSender;
import com.cat2bug.system.service.im.ImPushException;
import com.cat2bug.system.service.im.WeComClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImNoticeDispatchServiceTest {

    private final Map<Long, IMUserConfig> userConfigs = new HashMap<>();
    private final Map<String, Object> apps = new HashMap<>();
    private final Map<Long, String> userEmails = new HashMap<>();
    private final List<String> calls = Collections.synchronizedList(new ArrayList<>());
    private final List<SysNotice> notices = new ArrayList<>();
    private final List<String> sockets = new ArrayList<>();
    private boolean mailConfigured = true;
    private boolean dingGroupFails;

    private ImNoticeDispatchService service;

    @BeforeEach
    void setUp() {
        service = new ImNoticeDispatchService();
        service.imUserConfigService = new ImUserConfigService() {
            @Override
            public IMUserConfig selectImUserConfigByProjectAndMember(Long projectId, Long memberId,
                                                                     Map<String, Object> defaultOption) {
                IMUserConfig config = userConfigs.get(memberId);
                if (config == null) {
                    throw new IllegalStateException("配置无法解析");
                }
                return config;
            }
        };
        service.sysNoticeService = new SysNoticeService() {
            @Override
            public int insertReceiverNotice(SysNotice notice) {
                notices.add(notice);
                return 1;
            }
        };
        service.imWebSocketNotifier = new ImWebSocketNotifier() {
            @Override
            public void pushNotice(Long receiveMemberId, Object noticePayload) {
                sockets.add("notice:" + receiveMemberId);
            }

            @Override
            public void pushPanelNotice(Long receiveMemberId, Object noticePayload) {
                sockets.add("panel:" + receiveMemberId);
            }
        };
        service.imProjectConfigService = new ImProjectConfigService() {
            @Override
            public <T> T selectAppConfig(Long projectId, String systemCode, Class<T> type) {
                return type.cast(apps.get(systemCode));
            }
        };
        service.sysUserService = new SysUserService() {
            @Override
            public SysUser selectUserById(Long userId) {
                SysUser user = new SysUser();
                user.setEmail(userEmails.get(userId));
                return user;
            }
        };
        service.imLinkBuilder = new ImLinkBuilder();
        service.imMailSender = new ImMailSender() {
            @Override
            public boolean isConfigured() {
                return mailConfigured;
            }

            @Override
            public boolean send(String to, String subject, String html) {
                calls.add("mail:" + to + "|" + subject + "|" + html);
                return true;
            }
        };
        service.dingClient = new DingClient() {
            @Override
            public void sendGroupText(String hook, String secret, String content) throws Exception {
                if (dingGroupFails) {
                    throw new ImPushException("钉钉接口超时");
                }
                calls.add("ding-group:" + hook + "|" + secret + "|" + content);
            }

            @Override
            public void sendRobotActionCard(DingProjectConfig app, String mobile, String title, String markdown,
                                            String buttonTitle, String buttonUrl) {
                calls.add("ding-single:" + mobile + "|" + title + "|" + markdown + "|" + buttonTitle + "|" + buttonUrl);
            }
        };
        service.feishuClient = new FeishuClient() {
            @Override
            public void sendGroupText(String hook, String secret, String text) {
                calls.add("feishu-group:" + hook + "|" + text);
            }

            @Override
            public void sendUserText(FeishuProjectConfig app, String mobile, String text) {
                calls.add("feishu-single:" + mobile + "|" + text);
            }
        };
        service.weComClient = new WeComClient() {
            @Override
            public void sendGroupMarkdown(String hook, String content, String mentionMobile) {
                calls.add("wechat-group:" + hook + "|" + content + "|" + mentionMobile);
            }

            @Override
            public void sendAppMessage(EnterpriseWeChatProjectConfig app, String mobile, String msgType, String content) {
                calls.add("wechat-single:" + mobile + "|" + msgType + "|" + content);
            }
        };
        service.imPushTaskExecutor = new DirectExecutorService();
    }

    @Test
    void onlySystemSwitchWritesSiteNoticeWithoutExternalCalls() {
        userConfigs.put(1L, config(true));

        service.dispatch(event(List.of(1L), "defect"));

        assertEquals(1, notices.size());
        assertEquals("## md", notices.get(0).getNoticeContent());
        assertEquals("defect", notices.get(0).getGroupName());
        assertEquals(List.of("notice:1", "panel:1"), sockets);
        assertTrue(calls.isEmpty(), calls.toString());
    }

    @Test
    void dingSingleSendsActionCardWithFullScreenLink() {
        IMUserConfig cfg = config(false);
        cfg.getConfig().getPlatforms().setDing(new IMDingPlatformConfig(true, true, false, null, null, null, "13800000001"));
        userConfigs.put(1L, cfg);
        apps.put(DingProjectConfig.SYSTEM_CODE, new DingProjectConfig());

        service.dispatch(event(List.of(1L), "defect"));

        assertEquals(List.of("ding-single:13800000001|标题|## md|点击查看详情|http://h/t/p/defect?projectId=1&defectId=2&screen=full"),
                calls);
        assertTrue(notices.isEmpty());
    }

    @Test
    void missingProjectAppSkipsPersonalMessageButOtherChannelsContinue() {
        IMUserConfig cfg = config(true);
        cfg.getConfig().getPlatforms().setFeishu(new FeishuPlatformConfig(true, true, false, "13800000001", null, null, null));
        cfg.getConfig().getPlatforms().setMail(new IMMailPlatformConfig(true, "a@example.com"));
        userConfigs.put(1L, cfg);

        service.dispatch(event(List.of(1L), "defect"));

        assertEquals(1, notices.size());
        assertEquals(List.of("mail:a@example.com|标题|<p>html</p>"), calls);
    }

    @Test
    void groupMessagesAreSentOncePerEventUsingFirstRecipientConfig() {
        for (long id = 1; id <= 3; id++) {
            IMUserConfig cfg = config(false);
            IMPlatformConfig platforms = cfg.getConfig().getPlatforms();
            platforms.setDing(new IMDingPlatformConfig(true, false, true, "cat2bug", "S" + id, "http://ding/" + id, null));
            platforms.setFeishu(new FeishuPlatformConfig(true, false, true, null, null, null, "http://feishu/" + id));
            EnterpriseWeChatPlatformConfig weChat = new EnterpriseWeChatPlatformConfig();
            weChat.setGroupSwitch(true);
            weChat.setHook("http://wecom/" + id);
            weChat.setMobile("1380000000" + id);
            platforms.setWechat(weChat);
            userConfigs.put(id, cfg);
        }

        service.dispatch(event(List.of(1L, 2L, 3L), "defect"));

        assertEquals(List.of(
                "ding-group:http://ding/1|S1|【cat2bug】text",
                "feishu-group:http://feishu/1|text",
                "wechat-group:http://wecom/1|## md|13800000001"), calls);
    }

    @Test
    void weComGroupDoesNotRequireProjectAppConfig() {
        IMUserConfig cfg = config(false);
        EnterpriseWeChatPlatformConfig weChat = new EnterpriseWeChatPlatformConfig();
        weChat.setGroupSwitch(true);
        weChat.setHook("http://wecom/hook");
        cfg.getConfig().getPlatforms().setWechat(weChat);
        userConfigs.put(1L, cfg);
        apps.remove(EnterpriseWeChatProjectConfig.SYSTEM_CODE);

        service.dispatch(event(List.of(1L), "defect"));

        assertEquals(List.of("wechat-group:http://wecom/hook|## md|null"), calls);
    }

    @Test
    void failingChannelDoesNotAffectOtherChannelsOrCaller() {
        dingGroupFails = true;
        IMUserConfig cfg = config(true);
        IMPlatformConfig platforms = cfg.getConfig().getPlatforms();
        platforms.setDing(new IMDingPlatformConfig(true, false, true, null, null, "http://ding/1", null));
        platforms.setFeishu(new FeishuPlatformConfig(true, false, true, null, null, null, "http://feishu/1"));
        platforms.setMail(new IMMailPlatformConfig(true, null));
        userConfigs.put(1L, cfg);
        userEmails.put(1L, "user1@example.com");

        assertDoesNotThrow(() -> service.dispatch(event(List.of(1L), "defect")));

        assertEquals(1, notices.size());
        assertEquals(List.of("feishu-group:http://feishu/1|text", "mail:user1@example.com|标题|<p>html</p>"), calls);
    }

    @Test
    void mailIsSkippedWhenNotConfigured() {
        mailConfigured = false;
        IMUserConfig cfg = config(false);
        cfg.getConfig().getPlatforms().setMail(new IMMailPlatformConfig(true, "a@example.com"));
        userConfigs.put(1L, cfg);

        service.dispatch(event(List.of(1L), "defect"));

        assertTrue(calls.isEmpty());
    }

    @Test
    void recipientWithRealtimeOffOrBrokenConfigIsSkipped() {
        IMUserConfig off = config(true);
        off.getConfig().setModules(Map.of("defect", Map.of("option", Map.of("realtime", Map.of("switch", false)))));
        userConfigs.put(1L, off);
        userConfigs.put(3L, config(true));

        service.dispatch(event(List.of(1L, 2L, 3L), "defect"));

        assertEquals(1, notices.size());
        assertEquals(3L, notices.get(0).getReceiverId());
    }

    private static ImEvent event(List<Long> receivers, String moduleKey) {
        return new ImEvent(1L, "defect", 9L, receivers, "标题", "http://h/t/p/defect?projectId=1&defectId=2",
                new ImMessageBody("text", "## md", "<p>html</p>"), moduleKey,
                ImNoticeDefaults.defaultDefectNoticeOption());
    }

    private static IMUserConfig config(boolean system) {
        IMPlatformConfig platforms = new IMPlatformConfig();
        platforms.setSystem(new IMSystemPlatformConfig(system, false, null, true));
        platforms.setMail(new IMMailPlatformConfig(false, null));
        IMConfig config = new IMConfig();
        config.setModules(ImNoticeDefaults.defaultDefectNoticeOption());
        config.setPlatforms(platforms);
        IMUserConfig userConfig = new IMUserConfig();
        userConfig.setConfig(config);
        return userConfig;
    }

    private static final class DirectExecutorService extends AbstractExecutorService {
        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }
}

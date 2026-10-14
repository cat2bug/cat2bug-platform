package com.cat2bug.admin.service;

import com.cat2bug.common.domain.SysUserProject;
import com.cat2bug.common.domain.im.IMConfig;
import com.cat2bug.common.domain.im.IMUserConfig;
import com.cat2bug.system.mapper.SysUserProjectMapper;
import com.cat2bug.system.service.ImUserConfigService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportImNoticeServiceTest {

    @Test
    void onlyUnlockedMembersWithNewReportEventAreReceivers() {
        Map<Long, IMUserConfig> configs = new HashMap<>();
        configs.put(1L, config(Map.of("report", Map.of("event", Map.of("new", true)))));
        configs.put(2L, config(Map.of("report", Map.of("event", Map.of("new", false)))));
        configs.put(3L, config(Map.of("report", Map.of("event", Map.of("new", true)))));
        configs.put(5L, config(Map.of()));

        ReportImNoticeService service = new ReportImNoticeService();
        service.sysUserProjectMapper = mapperReturning(List.of(
                member(1L, 0), member(2L, 0), member(3L, 1), member(4L, null), member(5L, 0)));
        service.imUserConfigService = new ImUserConfigService() {
            @Override
            public IMUserConfig selectImUserConfigByProjectAndMember(Long projectId, Long memberId,
                                                                     Map<String, Object> defaultOption) {
                IMUserConfig config = configs.get(memberId);
                if (config == null) {
                    throw new IllegalStateException("配置无法解析");
                }
                return config;
            }
        };

        assertEquals(List.of(1L), service.selectReceivers(10L));
    }

    private static SysUserProjectMapper mapperReturning(List<SysUserProject> members) {
        return (SysUserProjectMapper) Proxy.newProxyInstance(
                SysUserProjectMapper.class.getClassLoader(),
                new Class<?>[]{SysUserProjectMapper.class},
                (proxy, method, args) -> {
                    if ("selectSysUserProjectList".equals(method.getName())) {
                        return members;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static SysUserProject member(Long userId, Integer lock) {
        SysUserProject member = new SysUserProject();
        member.setUserId(userId);
        member.setProjectLock(lock);
        return member;
    }

    private static IMUserConfig config(Map<String, Object> modules) {
        IMConfig config = new IMConfig();
        config.setModules(modules);
        IMUserConfig userConfig = new IMUserConfig();
        userConfig.setConfig(config);
        return userConfig;
    }
}

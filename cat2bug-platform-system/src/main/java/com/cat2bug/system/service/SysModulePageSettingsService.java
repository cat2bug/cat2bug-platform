package com.cat2bug.system.service;

import com.cat2bug.common.domain.ModulePageSettings;
import com.cat2bug.common.domain.ModulePageSettingsResponse;
import com.cat2bug.common.domain.SysProject;
import com.cat2bug.common.domain.SysTeam;
import com.cat2bug.common.exception.ApiException;
import com.cat2bug.common.security.TeamModuleGates;
import com.cat2bug.common.util.ModulePageSettingsSupport;
import com.cat2bug.common.util.UploadLimits;
import com.cat2bug.system.mapper.SysProjectMapper;
import com.cat2bug.system.mapper.SysTeamMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SysModulePageSettingsService {

    static final long CACHE_TTL_MILLIS = 30_000L;

    private record Cached(String raw, long at) {
    }

    private final Map<Long, Cached> cache = new ConcurrentHashMap<>();

    @Inject
    SysTeamMapper sysTeamMapper;

    @Inject
    SysProjectMapper sysProjectMapper;

    @Inject
    SysConfigService sysConfigService;

    public ModulePageSettings getByTeamId(Long teamId) {
        return ModulePageSettingsSupport.parseOrDefault(loadRaw(teamId));
    }

    public ModulePageSettings getByProjectId(Long projectId) {
        return getByTeamId(resolveTeamIdByProject(projectId));
    }

    public ModulePageSettingsResponse viewByTeamId(Long teamId) {
        return toResponse(getByTeamId(teamId));
    }

    public ModulePageSettingsResponse viewByProjectId(Long projectId) {
        return toResponse(getByProjectId(projectId));
    }

    @Transactional
    public ModulePageSettingsResponse saveByTeamId(Long teamId, ModulePageSettings body) {
        loadRaw(teamId);
        int ceiling = systemMaxMb();
        ModulePageSettings normalized = ModulePageSettingsSupport.normalizeAndValidate(body, ceiling);
        SysTeam update = new SysTeam();
        update.setTeamId(teamId);
        update.setModulePageConfig(ModulePageSettingsSupport.toJson(normalized));
        if (sysTeamMapper.updateSysTeam(update) <= 0) {
            throw new ApiException("保存页面设置失败");
        }
        cache.remove(teamId);
        return toResponse(normalized);
    }

    public void invalidate(Long teamId) {
        if (teamId != null) {
            cache.remove(teamId);
        }
    }

    /** 项目 MCP 勾选与团队闸门求交；{@code case_plan} 关闭时同时去掉 case 与 plan。 */
    public List<String> retainEnabledMcpModules(Long teamId, List<String> modules) {
        if (modules == null || modules.isEmpty()) {
            return List.of();
        }
        ModulePageSettings settings;
        try {
            settings = getByTeamId(teamId);
        } catch (RuntimeException e) {
            return List.copyOf(modules);
        }
        List<String> out = new ArrayList<>();
        for (String key : modules) {
            String gate = TeamModuleGates.gateOfMcpKey(key);
            if (gate == null || settings.isEnabled(gate)) {
                out.add(key);
            }
        }
        return List.copyOf(out);
    }

    public int systemMaxMb() {
        return UploadLimits.parseSystemMb(sysConfigService.selectConfigByKey(UploadLimits.CONFIG_KEY));
    }

    public Long teamIdOfProject(Long projectId) {
        if (projectId == null) {
            return null;
        }
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, null);
        return project == null ? null : project.getTeamId();
    }

    private ModulePageSettingsResponse toResponse(ModulePageSettings settings) {
        int system = systemMaxMb();
        ModulePageSettingsResponse response = new ModulePageSettingsResponse();
        response.setSettings(settings);
        response.setSystemMaxUploadMb(system);
        response.setEffectiveMaxUploadMb(ModulePageSettingsSupport.effectiveMaxMb(settings, system));
        return response;
    }

    private String loadRaw(Long teamId) {
        requireTeam(teamId);
        long now = System.currentTimeMillis();
        Cached cached = cache.get(teamId);
        if (cached != null && now - cached.at() < CACHE_TTL_MILLIS) {
            return cached.raw();
        }
        SysTeam team = sysTeamMapper.selectSysTeamByTeamId(teamId);
        if (team == null) {
            throw new ApiException("团队不存在");
        }
        String raw = team.getModulePageConfig();
        cache.put(teamId, new Cached(raw, now));
        return raw;
    }

    private Long resolveTeamIdByProject(Long projectId) {
        if (projectId == null) {
            throw new ApiException("项目不能为空");
        }
        SysProject project = sysProjectMapper.selectSysProjectByProjectId(projectId, null);
        if (project == null || project.getTeamId() == null) {
            throw new ApiException("项目不存在");
        }
        return project.getTeamId();
    }

    private static void requireTeam(Long teamId) {
        if (teamId == null) {
            throw new ApiException("团队不能为空");
        }
    }
}
